package no1.payamax.api.config.http.r2dbc

import jakarta.persistence.Entity
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
			try {
				SchemaManagementToolCoordinator.process(
					metadata,
					registry,
					settings,
					DelayedDropRegistryNotAvailableImpl.INSTANCE,
				)
			} catch (mismatch: SchemaManagementException) {
				publishMigration(scratch)
				throw mismatch
			}
		} finally {
			scratch.delete()
			StandardServiceRegistryBuilder.destroy(registry)
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
