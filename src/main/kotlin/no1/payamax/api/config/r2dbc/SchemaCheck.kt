package no1.payamax.api.config.r2dbc

import jakarta.persistence.Entity
import org.hibernate.boot.Metadata
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.model.naming.Identifier
import org.hibernate.boot.model.naming.ImplicitForeignKeyNameSource
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl
import org.hibernate.boot.model.relational.ColumnOrderingStrategy
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.boot.spi.MetadataImplementor
import org.hibernate.cfg.AvailableSettings
import org.hibernate.dialect.temptable.TemporaryTableColumn
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment
import org.hibernate.mapping.Column
import org.hibernate.mapping.Constraint
import org.hibernate.mapping.PersistentClass
import org.hibernate.mapping.Table
import org.hibernate.mapping.UserDefinedObjectType
import org.hibernate.tool.schema.spi.SchemaManagementException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.env.Environment
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.stereotype.Component
import java.io.File
import java.sql.Array
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

@Component
class SchemaCheck(private val environment: Environment) : SmartInitializingSingleton {
	override fun afterSingletonsInstantiated() {
		val databaseAction = environment.getProperty(DATABASE_ACTION) ?: "none"
		val scriptsAction = environment.getProperty(SCRIPTS_ACTION) ?: "none"
		if (databaseAction.equals("none", ignoreCase = true) && scriptsAction.equals("none", ignoreCase = true)) {
			return
		}

		val registry = StandardServiceRegistryBuilder()
			.applySetting(AvailableSettings.DIALECT, "org.hibernate.dialect.PostgreSQLDialect")
			.applySetting(AvailableSettings.PREFERRED_INSTANT_JDBC_TYPE, instantJdbcType())
			.applySetting(AvailableSettings.PREFER_NATIVE_ENUM_TYPES, environment.getProperty(NATIVE_ENUMS) ?: "false")
			.build()
		try {
			val sources = MetadataSources(registry)
			entityClasses().forEach(sources::addAnnotatedClass)
			val metadata = sources.metadataBuilder
				.applyImplicitNamingStrategy(ReadableForeignKeyNamingStrategy())
				.applyPhysicalNamingStrategy(EntitySuffixPhysicalNamingStrategy())
				.applyColumnOrderingStrategy(EntityFieldColumnOrderingStrategy())
				.build()
			val required = prepareModel(metadata)
			val actual = readDatabase(required)
			val statements = migrationStatements(required, actual)
			if (statements.isEmpty()) return
			val scratch = File.createTempFile("schema-", ".sql")
			try {
				scratch.writeText(statements.joinToString(separator = "\n", postfix = "\n"))
				publishMigration(scratch)
			} finally {
				scratch.delete()
			}
			throw SchemaManagementException(statements.joinToString("\n"))
		} finally {
			StandardServiceRegistryBuilder.destroy(registry)
		}
	}

	private fun readDatabase(required: SchemaModel): SchemaModel {
		DriverManager.getConnection(
			environment.getRequiredProperty("spring.datasource.url"),
			environment.getRequiredProperty("spring.datasource.username"),
			environment.getRequiredProperty("spring.datasource.password"),
		).use { connection ->
			val schemas = databaseSchemas(connection)
			val scope = (schemas + required.schemas).filterNot { systemSchema(it) }.distinct()
			return SchemaModel(
				schemas = schemas.filterNot { systemSchema(it) }.toSet(),
				sequences = databaseSequences(connection, scope),
				tables = databaseTables(connection, scope),
			)
		}
	}

	private fun databaseSchemas(connection: Connection): Set<String> {
		val found = linkedSetOf<String>()
		connection.createStatement().use { statement ->
			statement.executeQuery(
				"""
				select nspname
				from pg_catalog.pg_namespace
				where nspname <> 'information_schema' and nspname not like 'pg_%'
				order by nspname
				""".trimIndent(),
			).use { rows ->
				while (rows.next()) found += rows.getString(1)
			}
		}
		return found
	}

	private fun databaseSequences(connection: Connection, schemas: List<String>): Set<SequenceName> {
		val found = linkedSetOf<SequenceName>()
		connection.prepareStatement(
			"""
			select n.nspname, c.relname
			from pg_catalog.pg_class c
			join pg_catalog.pg_namespace n on n.oid = c.relnamespace
			where c.relkind = 'S' and n.nspname = any (?)
			""".trimIndent(),
		).use { statement ->
			statement.setArray(1, connection.createArrayOf("text", schemas.toTypedArray()))
			statement.executeQuery().use { rows ->
				while (rows.next()) found += SequenceName(rows.getString(1), rows.getString(2))
			}
		}
		return found
	}

	private fun databaseTables(connection: Connection, schemas: List<String>): List<TableModel> {
		val columns = linkedMapOf<Pair<String, String>, MutableList<ColumnModel>>()
		connection.prepareStatement(
			"""
			select n.nspname, c.relname, a.attname,
				pg_catalog.format_type(a.atttypid, a.atttypmod) as sql_type,
				a.attnotnull,
				pg_get_expr(ad.adbin, ad.adrelid) as column_default
			from pg_catalog.pg_attribute a
			join pg_catalog.pg_class c on c.oid = a.attrelid
			join pg_catalog.pg_namespace n on n.oid = c.relnamespace
			left join pg_catalog.pg_attrdef ad on ad.adrelid = a.attrelid and ad.adnum = a.attnum
			where c.relkind = 'r'
				and a.attnum > 0
				and not a.attisdropped
				and n.nspname = any (?)
				and c.relname <> 'flyway_schema_history'
			order by n.nspname, c.relname, a.attnum
			""".trimIndent(),
		).use { statement ->
			statement.setArray(1, connection.createArrayOf("text", schemas.toTypedArray()))
			statement.executeQuery().use { rows ->
				while (rows.next()) {
					val key = rows.getString(1) to rows.getString(2)
					columns.getOrPut(key) { mutableListOf() } += ColumnModel(
						name = rows.getString(3),
						sqlType = rows.getString(4),
						nullable = !rows.getBoolean(5),
						defaultSql = rows.getString(6),
					)
				}
			}
		}
		val constraints = databaseConstraints(connection, schemas)
		val indexes = databaseIndexes(connection, schemas)
		return columns.map { (key, cols) ->
			val owned = constraints.filter { it.schema == key.first && it.table == key.second }
			val primaryKey = owned.firstOrNull { it.kind == "p" }
			TableModel(
				schema = key.first,
				name = key.second,
				columns = cols,
				primaryKey = primaryKey?.columns ?: emptyList(),
				primaryKeyName = primaryKey?.name,
				uniques = owned.filter { it.kind == "u" }.map { KeyModel(it.name, it.columns) },
				foreignKeys = owned.filter { it.kind == "f" }.map {
					ForeignKeyModel(it.name, it.columns, it.referencedSchema ?: "public", it.referencedTable ?: "", it.referencedColumns)
				},
				checks = owned.filter { it.kind == "c" }.map { CheckModel(it.name, it.columns, it.definition) },
				indexes = indexes.filter { it.schema == key.first && it.table == key.second }
					.map { IndexModel(it.name, it.unique, it.columns.toList(), it.expression) },
			)
		}
	}

	private fun databaseConstraints(connection: Connection, schemas: List<String>): List<ConstraintRow> {
		val found = mutableListOf<ConstraintRow>()
		connection.prepareStatement(
			"""
			select n.nspname, t.relname, c.conname, c.contype,
				pg_get_constraintdef(c.oid) as definition,
				rn.nspname as ref_schema,
				rt.relname as ref_table,
				(select array_agg(a.attname order by k.ord)
					from unnest(c.conkey) with ordinality as k(attnum, ord)
					join pg_catalog.pg_attribute a on a.attrelid = t.oid and a.attnum = k.attnum) as columns,
				(select array_agg(a.attname order by k.ord)
					from unnest(c.confkey) with ordinality as k(attnum, ord)
					join pg_catalog.pg_attribute a on a.attrelid = rt.oid and a.attnum = k.attnum) as ref_columns
			from pg_catalog.pg_constraint c
			join pg_catalog.pg_class t on t.oid = c.conrelid
			join pg_catalog.pg_namespace n on n.oid = t.relnamespace
			left join pg_catalog.pg_class rt on rt.oid = c.confrelid
			left join pg_catalog.pg_namespace rn on rn.oid = rt.relnamespace
			where c.contype in ('p', 'u', 'f', 'c') and n.nspname = any (?)
			""".trimIndent(),
		).use { statement ->
			statement.setArray(1, connection.createArrayOf("text", schemas.toTypedArray()))
			statement.executeQuery().use { rows ->
				while (rows.next()) {
					found += ConstraintRow(
						schema = rows.getString(1),
						table = rows.getString(2),
						name = rows.getString(3),
						kind = rows.getString(4),
						definition = rows.getString(5) ?: "",
						referencedSchema = rows.getString(6),
						referencedTable = rows.getString(7),
						columns = textArray(rows.getArray(8)),
						referencedColumns = textArray(rows.getArray(9)),
					)
				}
			}
		}
		return found
	}

	private fun databaseIndexes(connection: Connection, schemas: List<String>): List<StoredIndex> {
		val found = linkedMapOf<Triple<String, String, String>, StoredIndex>()
		connection.prepareStatement(
			"""
			select n.nspname, t.relname, i.relname, ix.indisunique,
				(ix.indexprs is not null or ix.indpred is not null) as expression,
				a.attname
			from pg_catalog.pg_index ix
			join pg_catalog.pg_class i on i.oid = ix.indexrelid
			join pg_catalog.pg_class t on t.oid = ix.indrelid
			join pg_catalog.pg_namespace n on n.oid = t.relnamespace
			join lateral unnest(ix.indkey) with ordinality as k(attnum, ord)
				on k.ord <= ix.indnkeyatts
			left join pg_catalog.pg_attribute a
				on a.attrelid = t.oid and a.attnum = k.attnum and k.attnum > 0
			where n.nspname = any (?)
				and not ix.indisprimary
				and not exists (select 1 from pg_catalog.pg_constraint c where c.conindid = ix.indexrelid)
			order by n.nspname, t.relname, i.relname, k.ord
			""".trimIndent(),
		).use { statement ->
			statement.setArray(1, connection.createArrayOf("text", schemas.toTypedArray()))
			statement.executeQuery().use { rows ->
				while (rows.next()) {
					val key = Triple(rows.getString(1), rows.getString(2), rows.getString(3))
					val index = found.getOrPut(key) {
						StoredIndex(key.first, key.second, key.third, rows.getBoolean(4), rows.getBoolean(5), mutableListOf())
					}
					val column = rows.getString(6)
					if (column == null) index.expression = true else index.columns += column
				}
			}
		}
		return found.values.toList()
	}

	private fun textArray(array: java.sql.Array?): List<String> {
		if (array == null) return emptyList()
		val values = try {
			array.array
		} catch (_: java.sql.SQLException) {
			null
		}
		if (values is kotlin.Array<*>) return values.mapNotNull { it?.toString() }
		return pgArrayLiteral(array.toString())
	}

	private fun instantJdbcType(): String {
		val configured = environment.getProperty(INSTANT_JDBC_TYPE) ?: "TIMESTAMP"
		return when (configured.lowercase()) {
			"timestamptz", "timestamp_utc", "timestamp-with-time-zone" -> "TIMESTAMP_UTC"
			"timestamp" -> "TIMESTAMP"
			else -> configured
		}
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
		val migration = File(directory, migrationFileName(version, stamp, gitBranch()))
		scratch.copyTo(migration, overwrite = false)
		log.warn("Schema differs from the entity model. Wrote {}", migration.path)
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
}

private data class ConstraintRow(
	val schema: String,
	val table: String,
	val name: String,
	val kind: String,
	val definition: String,
	val referencedSchema: String?,
	val referencedTable: String?,
	val columns: List<String>,
	val referencedColumns: List<String>,
)

private data class StoredIndex(
	val schema: String,
	val table: String,
	val name: String,
	val unique: Boolean,
	var expression: Boolean,
	val columns: MutableList<String>,
)

internal data class SchemaModel(
	val schemas: Set<String>,
	val sequences: Set<SequenceName>,
	val tables: List<TableModel>,
)

internal data class SequenceName(val schema: String, val name: String)

internal data class TableModel(
	val schema: String,
	val name: String,
	val columns: List<ColumnModel> = emptyList(),
	val primaryKey: List<String> = emptyList(),
	val primaryKeyName: String? = null,
	val uniques: List<KeyModel> = emptyList(),
	val foreignKeys: List<ForeignKeyModel> = emptyList(),
	val checks: List<CheckModel> = emptyList(),
	val indexes: List<IndexModel> = emptyList(),
)

internal data class ColumnModel(
	val name: String,
	val sqlType: String,
	val nullable: Boolean,
	val defaultSql: String? = null,
)

internal data class KeyModel(val name: String, val columns: List<String>)

internal data class ForeignKeyModel(
	val name: String,
	val columns: List<String>,
	val referencedSchema: String,
	val referencedTable: String,
	val referencedColumns: List<String>,
)

internal data class CheckModel(val name: String?, val columns: List<String>, val expression: String)

internal data class IndexModel(
	val name: String,
	val unique: Boolean,
	val columns: List<String>,
	val expression: Boolean = false,
	val schema: String = "",
	val table: String = "",
)

internal fun prepareModel(metadata: Metadata): SchemaModel {
	applyKotlinNullability(metadata)
	(metadata as MetadataImplementor).orderColumns(true)
	return readEntityModel(metadata)
}

internal fun readEntityModel(metadata: Metadata): SchemaModel {
	val tables = metadata.database.namespaces.flatMap { namespace ->
		namespace.tables.filter { it.isPhysicalTable }.map { readTable(metadata, it) }
	}
	val sequences = tables.flatMap { table ->
		table.columns.mapNotNull { column -> sequenceReference(column.defaultSql, table.schema) }
	}.toSet()
	val schemas = (tables.map { it.schema } + sequences.map { it.schema }).toSet()
	return SchemaModel(schemas, sequences, tables)
}

private fun readTable(metadata: Metadata, table: Table): TableModel {
	val schema = physicalSchema(table.schema)
	val name = physicalName(table.name)
	val columns = table.columns.map { column ->
		ColumnModel(
			name = physicalName(column.canonicalName),
			sqlType = column.getSqlType(metadata),
			nullable = column.isNullable,
			defaultSql = column.defaultValue,
		)
	}
	val checks = columns.flatMap { column ->
		val mapped = table.columns.first { physicalName(it.canonicalName) == column.name }
		mapped.checkConstraints.map { CheckModel(it.name, listOf(column.name), it.constraint) }
	} + table.checks.map { CheckModel(it.name, emptyList(), it.constraint) }
	val primaryKey = table.primaryKey?.columns?.map { physicalName(it.canonicalName) } ?: emptyList()
	val uniques = table.uniqueKeys.values.mapNotNull { uniqueKey ->
		val keyName = uniqueKey.name?.let(::physicalName) ?: return@mapNotNull null
		KeyModel(keyName, uniqueKey.columns.map { physicalName(it.canonicalName) })
	}
	val foreignKeys = table.foreignKeys.values.mapNotNull { foreignKey ->
		val keyName = foreignKey.name?.let(::physicalName) ?: return@mapNotNull null
		val referenced = foreignKey.referencedTable
		val referencedColumns = foreignKey.referencedColumns.ifEmpty { referenced.primaryKey?.columns ?: emptyList() }
		ForeignKeyModel(
			keyName,
			foreignKey.columns.map { physicalName(it.canonicalName) },
			physicalSchema(referenced.schema),
			physicalName(referenced.name),
			referencedColumns.map { physicalName(it.canonicalName) },
		)
	}
	val indexes = table.indexes.values.mapNotNull { index ->
		val indexName = index.name?.let(::physicalName) ?: return@mapNotNull null
		IndexModel(indexName, index.isUnique, index.selectables.map { physicalName(it.text) })
	}
	return TableModel(schema, name, columns, primaryKey, null, uniques, foreignKeys, checks, indexes)
}

internal fun migrationStatements(required: SchemaModel, actual: SchemaModel): List<String> {
	val statements = mutableListOf<String>()
	val requiredTables = required.tables.associateBy { it.schema to it.name }
	val actualTables = actual.tables.associateBy { it.schema to it.name }
	val droppedSchemas = actual.schemas.filter { it !in required.schemas && it != "public" && !systemSchema(it) }.toSet()

	for (table in actual.tables.filter { (it.schema to it.name) !in requiredTables && it.schema !in droppedSchemas }) {
		statements += "drop table ${qualify(table.schema, table.name)} cascade;"
	}
	for ((key, table) in actualTables) {
		val model = requiredTables[key] ?: continue
		statements += dropForeignKeys(table, model)
		statements += dropIndexes(table, model)
		statements += dropChecks(table, model)
		statements += dropUniques(table, model)
		statements += dropPrimaryKey(table, model)
		statements += dropColumns(table, model)
	}
	for (sequence in actual.sequences.filter { it !in required.sequences && it.schema !in droppedSchemas }) {
		statements += "drop sequence ${qualify(sequence.schema, sequence.name)} cascade;"
	}
	for (schema in droppedSchemas.sorted()) statements += "drop schema ${quoteIdentifier(schema)} cascade;"

	for (schema in required.schemas.filter { it != "public" && it !in actual.schemas }.sorted()) {
		statements += "create schema ${quoteIdentifier(schema)};"
	}
	for (sequence in required.sequences.filter { it !in actual.sequences }.sortedWith(compareBy({ it.schema }, { it.name }))) {
		val start = if (sequence.name == "id") " start with 1000 increment by 1" else ""
		statements += "create sequence ${qualify(sequence.schema, sequence.name)}$start;"
	}
	for (table in tablesInDependencyOrder(required.tables)) {
		val existing = actualTables[table.schema to table.name]
		if (existing == null) {
			statements += createTable(table)
		} else {
			statements += alterColumns(table, existing)
		}
		statements += createPrimaryKey(table, existing)
		statements += createUniques(table, existing)
		statements += createForeignKeys(table, existing)
		statements += createChecks(table, existing)
		statements += createIndexes(table, existing)
	}
	return statements
}

private fun dropForeignKeys(actual: TableModel, required: TableModel): List<String> =
	actual.foreignKeys.filter { existing -> required.foreignKeys.none { sameForeignKey(it, existing) } }
		.map { "alter table ${qualify(actual.schema, actual.name)} drop constraint ${quoteIdentifier(it.name)};" }

private fun dropIndexes(actual: TableModel, required: TableModel): List<String> {
	val ownedByConstraint = (actual.uniques.map { it.name } + listOfNotNull(actual.primaryKeyName)).toSet()
	return actual.indexes.filter { existing ->
		existing.name !in ownedByConstraint && required.indexes.none { sameIndex(it, existing) }
	}.map { "drop index ${qualify(actual.schema, it.name)};" }
}

private fun dropChecks(actual: TableModel, required: TableModel): List<String> =
	actual.checks.filter { existing -> existing.name != null && required.checks.none { sameCheck(it, existing) } }
		.map { "alter table ${qualify(actual.schema, actual.name)} drop constraint ${quoteIdentifier(it.name!!)};" }

private fun dropUniques(actual: TableModel, required: TableModel): List<String> =
	actual.uniques.filter { existing -> required.uniques.none { sameUnique(it, existing) } }
		.map { "alter table ${qualify(actual.schema, actual.name)} drop constraint ${quoteIdentifier(it.name)};" }

private fun dropPrimaryKey(actual: TableModel, required: TableModel): List<String> {
	if (actual.primaryKey.isEmpty() || actual.primaryKey == required.primaryKey || actual.primaryKeyName == null) return emptyList()
	return listOf("alter table ${qualify(actual.schema, actual.name)} drop constraint ${quoteIdentifier(actual.primaryKeyName)};")
}

private fun dropColumns(actual: TableModel, required: TableModel): List<String> {
	val kept = required.columns.map { it.name }.toSet()
	return actual.columns.filter { it.name !in kept }
		.map { "alter table ${qualify(actual.schema, actual.name)} drop column ${quoteIdentifier(it.name)};" }
}

private fun createTable(table: TableModel): String {
	val columns = table.columns.joinToString(",\n\t") { column ->
		val checks = table.checks.filter { it.columns == listOf(column.name) }.joinToString("") { " check (${it.expression})" }
		"${quoteIdentifier(column.name)} ${columnDefinition(column)}$checks"
	}
	val primaryKey = if (table.primaryKey.isEmpty()) "" else ",\n\tprimary key (${table.primaryKey.joinToString(", ") { quoteIdentifier(it) }})"
	return "create table ${qualify(table.schema, table.name)} (\n\t$columns$primaryKey\n);"
}

private fun alterColumns(required: TableModel, actual: TableModel): List<String> {
	val existing = actual.columns.associateBy { it.name }
	val statements = mutableListOf<String>()
	for (column in required.columns) {
		val current = existing[column.name]
		val tableName = qualify(required.schema, required.name)
		val columnName = quoteIdentifier(column.name)
		if (current == null) {
			statements += "alter table $tableName add column $columnName ${columnDefinition(column)};"
			continue
		}
		if (canonicalType(current.sqlType) != canonicalType(column.sqlType)) {
			statements += "alter table $tableName alter column $columnName set data type ${column.sqlType};"
		}
		if (canonicalDefault(current.defaultSql) != canonicalDefault(column.defaultSql)) {
			statements += if (column.defaultSql == null) {
				"alter table $tableName alter column $columnName drop default;"
			} else {
				"alter table $tableName alter column $columnName set default ${column.defaultSql};"
			}
		}
		if (current.nullable != column.nullable) {
			val nullability = if (column.nullable) "drop not null" else "set not null"
			statements += "alter table $tableName alter column $columnName $nullability;"
		}
	}
	return statements
}

private fun createPrimaryKey(required: TableModel, actual: TableModel?): List<String> {
	if (required.primaryKey.isEmpty() || actual == null || actual.primaryKey == required.primaryKey) return emptyList()
	val columns = required.primaryKey.joinToString(", ") { quoteIdentifier(it) }
	return listOf("alter table ${qualify(required.schema, required.name)} add primary key ($columns);")
}

private fun createUniques(required: TableModel, actual: TableModel?): List<String> {
	val existing = actual?.uniques.orEmpty()
	return required.uniques.filter { desired -> existing.none { sameUnique(desired, it) } }
		.map { unique ->
			val columns = unique.columns.joinToString(", ") { quoteIdentifier(it) }
			"alter table ${qualify(required.schema, required.name)} add constraint ${quoteIdentifier(unique.name)} unique ($columns);"
		}
}

private fun createForeignKeys(required: TableModel, actual: TableModel?): List<String> {
	val existing = actual?.foreignKeys.orEmpty()
	return required.foreignKeys.filter { desired -> existing.none { sameForeignKey(desired, it) } }
		.map { foreignKey ->
			val columns = foreignKey.columns.joinToString(", ") { quoteIdentifier(it) }
			val referencedColumns = foreignKey.referencedColumns.joinToString(", ") { quoteIdentifier(it) }
			"alter table ${qualify(required.schema, required.name)} add constraint ${quoteIdentifier(foreignKey.name)} foreign key ($columns) references ${qualify(foreignKey.referencedSchema, foreignKey.referencedTable)} ($referencedColumns);"
		}
}

private fun sameUnique(required: KeyModel, actual: KeyModel): Boolean =
	required.name == actual.name && required.columns == actual.columns

private fun createChecks(required: TableModel, actual: TableModel?): List<String> {
	val existing = actual?.checks.orEmpty()
	return required.checks.filter { desired ->
		val createdWithTable = actual == null && desired.columns.size == 1
		!createdWithTable && existing.none { sameCheck(desired, it) }
	}.map { check ->
		val name = check.name?.let { "constraint ${quoteIdentifier(it)} " }.orEmpty()
		"alter table ${qualify(required.schema, required.name)} add ${name}check (${check.expression});"
	}
}

private fun tablesInDependencyOrder(tables: List<TableModel>): List<TableModel> {
	val byKey = tables.associateBy { it.schema to it.name }
	val ordered = mutableListOf<TableModel>()
	val visiting = mutableSetOf<Pair<String, String>>()
	val visited = mutableSetOf<Pair<String, String>>()
	fun visit(table: TableModel) {
		val key = table.schema to table.name
		if (key in visited) return
		check(visiting.add(key)) { "Schema check found a foreign key cycle at [${table.name}]" }
		for (foreignKey in table.foreignKeys) byKey[foreignKey.referencedSchema to foreignKey.referencedTable]?.let(::visit)
		visiting.remove(key)
		visited += key
		ordered += table
	}
	tables.sortedWith(compareBy({ it.schema }, { it.name })).forEach(::visit)
	return ordered
}

private fun createIndexes(required: TableModel, actual: TableModel?): List<String> {
	val existing = actual?.indexes.orEmpty()
	return required.indexes.filter { desired -> existing.none { sameIndex(desired, it) } }
		.map { index ->
			val unique = if (index.unique) "unique " else ""
			val columns = index.columns.joinToString(", ") { quoteIdentifier(it) }
			"create ${unique}index ${quoteIdentifier(index.name)} on ${qualify(required.schema, required.name)} ($columns);"
		}
}

private fun columnDefinition(column: ColumnModel): String {
	val defaultSql = column.defaultSql?.let { " default $it" } ?: ""
	val nullSql = if (column.nullable) "" else " not null"
	return "${column.sqlType}$defaultSql$nullSql"
}

private fun sameForeignKey(required: ForeignKeyModel, actual: ForeignKeyModel): Boolean =
	required.name == actual.name &&
		required.columns == actual.columns &&
		required.referencedSchema == actual.referencedSchema &&
		required.referencedTable == actual.referencedTable &&
		required.referencedColumns == actual.referencedColumns

private fun sameIndex(required: IndexModel, actual: IndexModel): Boolean =
	!actual.expression &&
		required.name == actual.name &&
		required.unique == actual.unique &&
		required.columns == actual.columns

private fun sameCheck(required: CheckModel, actual: CheckModel): Boolean {
	if (required.name != null && required.name != actual.name) return false
	if (required.columns.isNotEmpty() && actual.columns.isNotEmpty() && required.columns != actual.columns) return false
	val requiredLiterals = literals(required.expression)
	val actualLiterals = literals(actual.expression)
	if (requiredLiterals.isNotEmpty() && actualLiterals.isNotEmpty()) return requiredLiterals == actualLiterals
	return canonicalCheck(required.expression) == canonicalCheck(actual.expression)
}

private fun literals(expression: String): List<String> =
	Regex("'([^']*)'").findAll(expression).map { it.groupValues[1] }.sorted().toList()

internal fun canonicalType(sqlType: String): String {
	var type = sqlType.lowercase().trim()
	type = type.replace("character varying", "varchar")
	type = type.replace("timestamp without time zone", "timestamp")
	type = type.replace("timestamp with time zone", "timestamptz")
	type = type.replace("double precision", "float8")
	type = type.replace(Regex("""float\((\d+)\)""")) { match ->
		if (match.groupValues[1].toInt() > 24) "float8" else "float4"
	}
	type = type.replace(Regex("""timestamp\(\d+\)"""), "timestamp")
	type = type.replace(Regex("""timestamptz\(\d+\)"""), "timestamptz")
	type = type.replace(Regex("""\breal\b"""), "float4")
	type = type.replace(Regex("""\bbigint\b"""), "int8")
	type = type.replace(Regex("""\binteger\b"""), "int4")
	type = type.replace(Regex("""\bsmallint\b"""), "int2")
	type = type.replace(Regex("""\bboolean\b"""), "bool")
	return type.replace(Regex("""\s+"""), " ").trim()
}

internal fun canonicalDefault(expression: String?): String? {
	if (expression.isNullOrBlank()) return null
	return expression.lowercase()
		.replace("::regclass", "")
		.replace(Regex("""::[a-z_][a-z0-9_ ]*"""), "")
		.replace(Regex("""\s+"""), " ")
		.trim()
}

internal fun canonicalCheck(expression: String): String {
	var text = expression.lowercase().replace(Regex("""::[a-z_][a-z0-9_ ]*"""), "")
	text = text.removePrefix("check").trim()
	text = text.replace(Regex("""\s+"""), " ")
	while (text.startsWith("(") && text.endsWith(")") && parenBalanced(text.substring(1, text.lastIndex))) {
		text = text.substring(1, text.lastIndex).trim()
	}
	return text
}

private fun parenBalanced(text: String): Boolean {
	var depth = 0
	for (char in text) {
		if (char == '(') depth++
		if (char == ')') depth--
		if (depth < 0) return false
	}
	return depth == 0
}

private fun pgArrayLiteral(text: String): List<String> {
	val body = text.trim().removePrefix("{").removeSuffix("}")
	if (body.isBlank() || body.equals("null", ignoreCase = true)) return emptyList()
	return body.split(',').map { it.trim().trim('"') }.filter { it.isNotEmpty() }
}

private fun sequenceReference(defaultSql: String?, tableSchema: String): SequenceName? {
	val match = Regex("""nextval\('(?:([A-Za-z_][A-Za-z0-9_]*)\.)?([A-Za-z_][A-Za-z0-9_]*)'\)""").find(defaultSql ?: return null)
		?: return null
	return SequenceName(match.groupValues[1].ifEmpty { tableSchema }, match.groupValues[2])
}

private fun applyKotlinNullability(metadata: Metadata) {
	for (binding in metadata.entityBindings) {
		val entity = binding.mappedClass
		val identifier = binding.identifierProperty
			?: error("Schema check found no id property on [${binding.entityName}]")
		for (column in identifier.columns) column.isNullable = databaseNullable(entity, identifier.name)
		for (property in binding.properties) {
			val nullable = databaseNullable(entity, property.name)
			for (column in property.columns) column.isNullable = nullable
		}
	}
}

private fun physicalSchema(schema: String?): String = schema?.trim('"')?.takeIf { it.isNotEmpty() } ?: "public"

private fun physicalName(name: String): String = name.trim('"').lowercase()

private fun qualify(schema: String, name: String): String = "${quoteIdentifier(schema)}.${quoteIdentifier(name)}"

private fun systemSchema(name: String): Boolean = name == "information_schema" || name.startsWith("pg_")

private fun quoteIdentifier(identifier: String): String {
	require(identifier.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) { "Refusing to emit an identifier: $identifier" }
	return "\"$identifier\""
}

internal fun migrationFileName(version: Int, stamp: String, branch: String): String {
	val suffix = branch.replace(Regex("[^A-Za-z0-9_-]+"), "_").trim('_')
	check(suffix.isNotEmpty()) { "Schema check cannot use git branch [$branch] as a migration suffix" }
	return "V${version.toString().padStart(5, '0')}__${stamp}_$suffix.sql"
}

private fun gitBranch(): String {
	val process = try {
		ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD")
			.redirectError(ProcessBuilder.Redirect.DISCARD)
			.start()
	} catch (failure: java.io.IOException) {
		throw IllegalStateException("Schema check could not read the current git branch", failure)
	}
	val branch = process.inputStream.bufferedReader().use { it.readText() }.trim()
	check(process.waitFor() == 0 && branch.isNotEmpty()) { "Schema check could not read the current git branch" }
	return branch
}

internal fun databaseNullable(entity: Class<*>, propertyName: String): Boolean {
	if (propertyName == "id") return false
	val marked = entity.kotlin.memberProperties
		.filter { it.name == propertyName }
		.map { it.returnType.isMarkedNullable }
		.distinct()
	check(marked.size == 1) {
		"Schema check found ${marked.size} nullability declarations for [${entity.simpleName}.$propertyName]"
	}
	return marked.single()
}

internal class EntityFieldColumnOrderingStrategy : ColumnOrderingStrategy {
	override fun orderTableColumns(table: Table, metadata: Metadata): List<Column>? {
		val binding = metadata.entityBindings.firstOrNull { it.table.exportIdentifier == table.exportIdentifier }
			?: return null
		val entity = binding.mappedClass ?: return null
		val names = entity.kotlin.primaryConstructor?.parameters?.mapNotNull { it.name }.orEmpty()
		if (names.isEmpty()) return null
		val remaining = ArrayList(table.columns)
		val ordered = ArrayList<Column>(remaining.size)
		for (name in names) {
			val property = persistentProperty(binding, name) ?: continue
			for (column in property.columns) {
				if (column.value.table.exportIdentifier != table.exportIdentifier) continue
				val index = remaining.indexOf(column)
				if (index >= 0) ordered += remaining.removeAt(index)
			}
		}
		ordered += remaining
		return ordered
	}

	override fun orderConstraintColumns(constraint: Constraint, metadata: Metadata): List<Column>? {
		val ordered = orderTableColumns(constraint.table, metadata) ?: return null
		val rank = HashMap<Column, Int>(ordered.size)
		ordered.forEachIndexed { index, column -> rank.putIfAbsent(column, index) }
		return constraint.columns.sortedBy { rank[it] ?: Int.MAX_VALUE }
	}

	override fun orderUserDefinedTypeColumns(
		userDefinedType: UserDefinedObjectType,
		metadata: Metadata,
	): List<Column>? = null

	override fun orderTemporaryTableColumns(temporaryTableColumns: MutableList<TemporaryTableColumn>, metadata: Metadata) =
		Unit

	private fun persistentProperty(binding: PersistentClass, name: String) =
		if (binding.identifierProperty?.name == name) binding.identifierProperty
		else binding.properties.find { it.name == name }
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
