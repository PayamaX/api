package no1.payamax.api.config.r2dbc

import jakarta.persistence.Entity
import org.hibernate.boot.Metadata
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.model.naming.Identifier
import org.hibernate.boot.model.naming.ImplicitForeignKeyNameSource
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.AvailableSettings
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment
import org.hibernate.tool.schema.spi.DelayedDropRegistryNotAvailableImpl
import org.hibernate.tool.schema.spi.SchemaManagementException
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.env.Environment
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.stereotype.Component
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class SchemaCheck(private val environment: Environment) : SmartInitializingSingleton {
	override fun afterSingletonsInstantiated() {
		val databaseAction = environment.getProperty(DATABASE_ACTION) ?: "none"
		val scriptsAction = environment.getProperty(SCRIPTS_ACTION) ?: "none"
		if (databaseAction.equals("none", ignoreCase = true) && scriptsAction.equals("none", ignoreCase = true)) {
			return
		}

		val settings = linkedMapOf<String, Any>(
			AvailableSettings.JAKARTA_JDBC_DRIVER to "org.postgresql.Driver",
			AvailableSettings.JAKARTA_JDBC_URL to environment.getRequiredProperty("spring.datasource.url"),
			AvailableSettings.JAKARTA_JDBC_USER to environment.getRequiredProperty("spring.datasource.username"),
			AvailableSettings.JAKARTA_JDBC_PASSWORD to environment.getRequiredProperty("spring.datasource.password"),
			AvailableSettings.DIALECT to "org.hibernate.dialect.PostgreSQLDialect",
			AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION to databaseAction,
			AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION to scriptsAction,
			AvailableSettings.HBM2DDL_HALT_ON_ERROR to "true",
			AvailableSettings.HBM2DDL_SCRIPTS_CREATE_APPEND to "false",
			AvailableSettings.PREFERRED_INSTANT_JDBC_TYPE to instantJdbcType(),
			AvailableSettings.PREFER_NATIVE_ENUM_TYPES to (environment.getProperty(NATIVE_ENUMS) ?: "false"),
		)
		val scratch = File.createTempFile("schema-", ".sql")
		settings[AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET] = scratch.absolutePath

		val registry = StandardServiceRegistryBuilder().applySettings(settings).build()
		try {
			val sources = MetadataSources(registry)
			entityClasses().forEach(sources::addAnnotatedClass)
			val metadata = sources.metadataBuilder
				.applyImplicitNamingStrategy(ReadableForeignKeyNamingStrategy())
				.applyPhysicalNamingStrategy(EntitySuffixPhysicalNamingStrategy())
				.build()
			var mismatch: SchemaManagementException? = null
			try {
				SchemaManagementToolCoordinator.process(
					metadata,
					registry,
					settings,
					DelayedDropRegistryNotAvailableImpl.INSTANCE,
				)
			} catch (failure: SchemaManagementException) {
				mismatch = failure
			}
			val unmapped = unmappedColumns(metadata)
			appendUnmappedColumns(scratch, unmapped)
			val messages = buildList {
				mismatch?.message?.let { add(it) }
				if (unmapped.isNotEmpty()) add(unmappedMessage(unmapped))
			}
			if (messages.isNotEmpty()) {
				publishMigration(scratch)
				throw SchemaManagementException(messages.joinToString("\n"))
			}
		} finally {
			scratch.delete()
			StandardServiceRegistryBuilder.destroy(registry)
		}
	}

	private fun unmappedColumns(metadata: Metadata): List<UnmappedColumn> {
		val mapped = mappedTables(metadata)
		if (mapped.isEmpty()) return emptyList()
		DriverManager.getConnection(
			environment.getRequiredProperty("spring.datasource.url"),
			environment.getRequiredProperty("spring.datasource.username"),
			environment.getRequiredProperty("spring.datasource.password"),
		).use { connection ->
			val found = mutableListOf<UnmappedColumn>()
			connection.prepareStatement(
				"""
				select table_schema, table_name, column_name
				from information_schema.columns
				where table_schema = any (?) and table_name = any (?)
				order by table_schema, table_name, ordinal_position
				""".trimIndent(),
			).use { statement ->
				statement.setArray(1, connection.createArrayOf("text", mapped.map { it.schema }.distinct().toTypedArray()))
				statement.setArray(2, connection.createArrayOf("text", mapped.map { it.table }.distinct().toTypedArray()))
				statement.executeQuery().use { rows ->
					while (rows.next()) {
						val schema = rows.getString(1)
						val table = rows.getString(2)
						val column = rows.getString(3)
						val match = mapped.find { it.schema == schema && it.table == table } ?: continue
						if (column in match.columns) continue
						found += UnmappedColumn(schema, table, column, columnHasValue(connection, schema, table, column))
					}
				}
			}
			return found
		}
	}

	private fun mappedTables(metadata: Metadata): List<MappedTable> {
		val tables = linkedMapOf<Pair<String, String>, MutableSet<String>>()
		for (namespace in metadata.database.namespaces) {
			for (table in namespace.tables) {
				if (!table.isPhysicalTable) continue
				val schema = table.schema?.trim('"')?.takeIf { it.isNotEmpty() } ?: "public"
				val name = table.name.trim('"').lowercase()
				val columns = tables.getOrPut(schema to name) { linkedSetOf() }
				for (column in table.columns) {
					columns += column.canonicalName.trim('"')
				}
			}
		}
		return tables.map { (key, columns) -> MappedTable(key.first, key.second, columns) }
	}

	private fun columnHasValue(connection: Connection, schema: String, table: String, column: String): Boolean {
		val sql = "select exists (select 1 from ${quote(schema)}.${quote(table)} where ${quote(column)} is not null)"
		connection.createStatement().use { statement ->
			statement.executeQuery(sql).use { rows ->
				rows.next()
				return rows.getBoolean(1)
			}
		}
	}

	private fun appendUnmappedColumns(scratch: File, columns: List<UnmappedColumn>) {
		if (columns.isEmpty()) return
		val script = columns.joinToString(separator = "\n") { column ->
			"""
			do $$
			begin
				if exists (select 1 from ${quote(column.schema)}.${quote(column.table)} where ${quote(column.column)} is not null) then
					raise exception 'column ${column.table}.${column.column} has a non-null value';
				end if;
				alter table ${quote(column.schema)}.${quote(column.table)} drop column ${quote(column.column)};
			end
			$$;
			""".trimIndent()
		}
		val existing = scratch.readText()
		val separator = if (existing.isEmpty() || existing.endsWith("\n")) "" else "\n"
		scratch.appendText(separator + script + "\n")
	}

	private fun unmappedMessage(columns: List<UnmappedColumn>): String = columns.joinToString("\n") { column ->
		val state = if (column.hasValue) "has a non-null value" else "is null for every row"
		"Schema validation: unmapped column [${column.column}] in table [${column.table}] $state"
	}

	private fun quote(identifier: String): String {
		require(identifier.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) { "Refusing to emit an identifier: $identifier" }
		return "\"$identifier\""
	}

	private fun publishMigration(scratch: File) {
		if (!scratch.isFile || scratch.readLines().none { line ->
				val text = line.trim()
				text.isNotEmpty() && !text.startsWith("--")
			}
		) {
			return
		}
		val directory = File(MIGRATION_DIR)
		if (!directory.isDirectory) return
		val version = (directory.listFiles()?.mapNotNull { file ->
			VERSION.matchEntire(file.name)?.groupValues?.get(1)?.toIntOrNull()
		}?.maxOrNull() ?: 0) + 1
		val stamp = LocalDateTime.now().format(STAMP)
		val migration = File(directory, "V${version.toString().padStart(5, '0')}__$stamp.sql")
		scratch.copyTo(migration, overwrite = false)
		log.warn("Schema differs from the entity model. Wrote {}", migration.path)
	}

	private fun instantJdbcType(): String {
		val configured = environment.getProperty(INSTANT_JDBC_TYPE) ?: "TIMESTAMP"
		return when (configured.lowercase()) {
			"timestamptz", "timestamp_utc", "timestamp-with-time-zone" -> "TIMESTAMP_UTC"
			"timestamp" -> "TIMESTAMP"
			else -> configured
		}
	}

	private fun entityClasses(): List<Class<*>> {
		val scanner = ClassPathScanningCandidateComponentProvider(false)
		scanner.addIncludeFilter(AnnotationTypeFilter(Entity::class.java))
		val classes = scanner.findCandidateComponents(BASE_PACKAGE).map { Class.forName(it.beanClassName) }
		check(classes.isNotEmpty()) { "Schema check found no @Entity classes under $BASE_PACKAGE" }
		return classes
	}

	private companion object {
		const val BASE_PACKAGE = "no1.payamax.api"
		const val MIGRATION_DIR = "src/main/resources/db/migration"
		const val DATABASE_ACTION = "spring.jpa.properties.jakarta.persistence.schema-generation.database.action"
		const val SCRIPTS_ACTION = "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action"
		const val INSTANT_JDBC_TYPE = "spring.jpa.properties.hibernate.type.preferred_instant_jdbc_type"
		const val NATIVE_ENUMS = "spring.jpa.properties.hibernate.type.prefer_native_enum_types"
		val VERSION = Regex("""V(\d+)__.*\.sql""")
		val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyMMdd_HHmm")
		val log = LoggerFactory.getLogger(SchemaCheck::class.java)
	}

	private data class MappedTable(val schema: String, val table: String, val columns: Set<String>)

	private data class UnmappedColumn(val schema: String, val table: String, val column: String, val hasValue: Boolean)
}

internal class ReadableForeignKeyNamingStrategy : ImplicitNamingStrategyJpaCompliantImpl() {
	override fun determineForeignKeyName(source: ImplicitForeignKeyNameSource): Identifier {
		source.userProvidedIdentifier?.let { return it }
		val table = readable(source.tableName)
		val columns = source.columnNames.joinToString("__") { readable(it) }
		return Identifier.toIdentifier("fk__${table}__${columns}")
	}

	private fun readable(name: Identifier): String {
		if (name.isQuoted) return name.text
		val builder = StringBuilder(name.text.replace('.', '_'))
		var index = 1
		while (index < builder.length - 1) {
			val before = builder[index - 1]
			val current = builder[index]
			val after = builder[index + 1]
			if ((before.isLowerCase() || before.isDigit()) && current.isUpperCase() && (after.isLowerCase() || after.isDigit())) {
				builder.insert(index, '_')
				index++
			}
			index++
		}
		return builder.toString().lowercase().removeSuffix("_entity")
	}
}

internal class EntitySuffixPhysicalNamingStrategy : PhysicalNamingStrategySnakeCaseImpl() {
	override fun toPhysicalTableName(logicalName: Identifier, jdbcEnvironment: JdbcEnvironment): Identifier {
		val snake = super.toPhysicalTableName(logicalName, jdbcEnvironment)
		return Identifier.toIdentifier(snake.text.removeSuffix("_entity"), snake.isQuoted)
	}
}
