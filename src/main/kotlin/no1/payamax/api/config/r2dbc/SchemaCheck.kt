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
			// Hibernate does not validate indexes unless this is set, so a missing index
			// never fails the check and the update script that creates it is discarded.
			AvailableSettings.INDEX_VALIDATION to "ALL",
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
			val indexes = indexChanges(metadata, scratch, validationFailed = mismatch != null)
			appendIndexChanges(scratch, indexes)
			val messages = buildList {
				mismatch?.message?.let { add(it) }
				if (unmapped.isNotEmpty()) add(unmappedMessage(unmapped))
				if (indexes.isNotEmpty()) add(indexMessage(indexes))
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

	private fun indexChanges(metadata: Metadata, scratch: File, validationFailed: Boolean): List<IndexChange> {
		val tables = mappedTables(metadata)
		if (tables.isEmpty()) return emptyList()
		val mapped = mappedIndexes(metadata)
		val actual = databaseIndexes(tables)
		val script = scratch.readText()
		val changes = mutableListOf<IndexChange>()
		for (desired in mapped.values) {
			val existing = actual[IndexKey(desired.schema, desired.table, desired.name)]
			if (existing == null) {
				val alreadyScripted = scriptCreates(script, desired.name)
				if (alreadyScripted && validationFailed) continue
				changes += IndexChange(
					desired.schema,
					desired.table,
					desired.name,
					if (alreadyScripted) emptyList() else listOf(createIndex(desired)),
					"Schema validation: missing index [${desired.name}] in table [${desired.table}]",
				)
				continue
			}
			if (existing.matches(desired)) continue
			changes += IndexChange(
				desired.schema,
				desired.table,
				desired.name,
				listOf(dropIndex(existing), createIndex(desired)),
				"Schema validation: index [${desired.name}] in table [${desired.table}] is ${existing.describe()} and the model declares ${desired.describe()}",
			)
		}
		for (existing in actual.values) {
			if (IndexKey(existing.schema, existing.table, existing.name) in mapped) continue
			changes += IndexChange(
				existing.schema,
				existing.table,
				existing.name,
				listOf(dropIndex(existing)),
				"Schema validation: unmapped index [${existing.name}] in table [${existing.table}]",
			)
		}
		return changes.sortedWith(compareBy({ it.schema }, { it.table }, { it.name }))
	}

	private fun mappedIndexes(metadata: Metadata): Map<IndexKey, MappedIndex> {
		val indexes = linkedMapOf<IndexKey, MappedIndex>()
		for (namespace in metadata.database.namespaces) {
			for (table in namespace.tables) {
				if (!table.isPhysicalTable) continue
				val schema = table.schema?.trim('"')?.takeIf { it.isNotEmpty() } ?: "public"
				val tableName = table.name.trim('"').lowercase()
				for (index in table.indexes.values) {
					val name = index.name?.trim('"')?.lowercase()?.takeIf { it.isNotEmpty() } ?: continue
					val columns = index.selectables.map { it.text.trim('"').lowercase() }
					val mapped = MappedIndex(schema, tableName, name, columns, index.isUnique)
					indexes[IndexKey(schema, tableName, name)] = mapped
				}
			}
		}
		return indexes
	}

	private fun databaseIndexes(tables: List<MappedTable>): Map<IndexKey, DatabaseIndex> {
		val found = linkedMapOf<IndexKey, DatabaseIndex>()
		DriverManager.getConnection(
			environment.getRequiredProperty("spring.datasource.url"),
			environment.getRequiredProperty("spring.datasource.username"),
			environment.getRequiredProperty("spring.datasource.password"),
		).use { connection ->
			connection.prepareStatement(
				"""
				select n.nspname as schema_name,
					t.relname as table_name,
					i.relname as index_name,
					ix.indisunique as is_unique,
					ix.indpred is not null as is_partial,
					a.attname as column_name
				from pg_catalog.pg_index ix
				join pg_catalog.pg_class i on i.oid = ix.indexrelid
				join pg_catalog.pg_class t on t.oid = ix.indrelid
				join pg_catalog.pg_namespace n on n.oid = t.relnamespace
				join lateral unnest(ix.indkey) with ordinality as k(attnum, ord)
					on k.ord <= ix.indnkeyatts
				left join pg_catalog.pg_attribute a
					on a.attrelid = t.oid and a.attnum = k.attnum and k.attnum > 0
				where n.nspname = any (?)
					and t.relname = any (?)
					and not ix.indisprimary
					and not exists (
						select 1 from pg_catalog.pg_constraint c where c.conindid = ix.indexrelid
					)
				order by n.nspname, t.relname, i.relname, k.ord
				""".trimIndent(),
			).use { statement ->
				statement.setArray(1, connection.createArrayOf("text", tables.map { it.schema }.distinct().toTypedArray()))
				statement.setArray(2, connection.createArrayOf("text", tables.map { it.table }.distinct().toTypedArray()))
				statement.executeQuery().use { rows ->
					while (rows.next()) {
						val schema = rows.getString("schema_name")
						val table = rows.getString("table_name")
						val name = rows.getString("index_name")
						val key = IndexKey(schema, table, name)
						val index = found.getOrPut(key) {
							DatabaseIndex(
								schema,
								table,
								name,
								rows.getBoolean("is_unique"),
								rows.getBoolean("is_partial"),
								false,
								mutableListOf(),
							)
						}
						val column = rows.getString("column_name")
						if (column == null) index.expression = true else index.columns += column.lowercase()
					}
				}
			}
		}
		return found
	}

	private fun scriptCreates(script: String, indexName: String): Boolean {
		val name = Regex.escape(indexName)
		return Regex("""(?i)create\s+(?:unique\s+)?index\s+(?:if\s+not\s+exists\s+)?(?:"$name"|$name)\b""")
			.containsMatchIn(script)
	}

	private fun createIndex(index: MappedIndex): String {
		val unique = if (index.unique) "unique " else ""
		val columns = index.columns.joinToString(", ") { quote(it) }
		return "create ${unique}index ${quote(index.name)} on ${quote(index.schema)}.${quote(index.table)} ($columns);"
	}

	private fun dropIndex(index: DatabaseIndex): String =
		"drop index if exists ${quote(index.schema)}.${quote(index.name)};"

	private fun appendIndexChanges(scratch: File, changes: List<IndexChange>) {
		val script = changes.flatMap { it.statements }.filter { it.isNotBlank() }.joinToString("\n")
		if (script.isBlank()) return
		val existing = scratch.readText()
		val separator = if (existing.isEmpty() || existing.endsWith("\n")) "" else "\n"
		scratch.appendText(separator + script + "\n")
	}

	private fun indexMessage(changes: List<IndexChange>): String =
		changes.joinToString("\n") { it.message }

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

	private data class IndexKey(val schema: String, val table: String, val name: String)

	private data class MappedIndex(
		val schema: String,
		val table: String,
		val name: String,
		val columns: List<String>,
		val unique: Boolean,
	) {
		fun describe(): String = indexDescription(unique, columns, expression = false)
	}

	private data class DatabaseIndex(
		val schema: String,
		val table: String,
		val name: String,
		val unique: Boolean,
		val partial: Boolean,
		var expression: Boolean,
		val columns: MutableList<String>,
	) {
		fun matches(mapped: MappedIndex): Boolean =
			!partial && !expression && unique == mapped.unique && columns == mapped.columns

		fun describe(): String = indexDescription(unique, columns, expression || partial)
	}

	private data class IndexChange(
		val schema: String,
		val table: String,
		val name: String,
		val statements: List<String>,
		val message: String,
	)
}

private fun indexDescription(unique: Boolean, columns: List<String>, expression: Boolean): String {
	val kind = if (unique) "unique " else ""
	val body = if (expression) "an expression" else columns.joinToString(", ")
	return "$kind($body)"
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
