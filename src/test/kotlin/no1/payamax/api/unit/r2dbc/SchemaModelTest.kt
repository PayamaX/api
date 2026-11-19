package no1.payamax.api.unit.r2dbc

import no1.libs.dbmigration.EntityFieldColumnOrderingStrategy
import no1.libs.dbmigration.EntitySuffixPhysicalNamingStrategy
import no1.libs.dbmigration.ReadableForeignKeyNamingStrategy
import no1.libs.dbmigration.SequenceName
import no1.libs.dbmigration.prepareModel
import no1.payamax.api.modules.payamakreport.PayamakReportEntity
import no1.payamax.api.modules.payamakreport.payamakreportruleprocessresult.PayamakReportRuleProcessResultEntity
import no1.payamax.api.modules.rankedorigin.RankedOriginEntity
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.AvailableSettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SchemaModelTest {
	@Test
	fun `entities describe the required database objects`() {
		val registry = StandardServiceRegistryBuilder()
			.applySetting(AvailableSettings.DIALECT, "org.hibernate.dialect.PostgreSQLDialect")
			.applySetting(AvailableSettings.PREFERRED_INSTANT_JDBC_TYPE, "TIMESTAMP")
			.applySetting(AvailableSettings.PREFER_NATIVE_ENUM_TYPES, "false")
			.build()
		try {
			val metadata = MetadataSources(registry)
				.addAnnotatedClass(RankedOriginEntity::class.java)
				.addAnnotatedClass(PayamakReportEntity::class.java)
				.addAnnotatedClass(PayamakReportRuleProcessResultEntity::class.java)
				.metadataBuilder
				.applyImplicitNamingStrategy(ReadableForeignKeyNamingStrategy())
				.applyPhysicalNamingStrategy(EntitySuffixPhysicalNamingStrategy())
				.applyColumnOrderingStrategy(EntityFieldColumnOrderingStrategy())
				.build()
			val model = prepareModel(metadata)
			val ranked = model.tables.first { it.name == "ranked_origin" }
			assertEquals(
				listOf("id", "global_id", "updated", "deleted", "origin", "rank", "owner_global_id", "position", "comment"),
				ranked.columns.map { it.name },
			)
			val id = ranked.columns.first()
			assertEquals(false, id.nullable)
			assertTrue(id.defaultSql?.contains("nextval('id')") == true, id.defaultSql)
			assertEquals(true, ranked.columns.first { it.name == "deleted" }.nullable)
			assertEquals(false, ranked.columns.first { it.name == "origin" }.nullable)
			assertTrue(model.sequences.contains(SequenceName("public", "id")))

			val report = model.tables.first { it.name == "payamak_report" }
			val stateCheck = report.checks.filter { it.columns == listOf("state") || it.expression.contains("PENDING") }
			assertTrue(stateCheck.isNotEmpty(), report.checks.toString())

			val result = model.tables.first { it.name == "payamak_report_rule_process_result" }
			assertEquals(listOf("payamak_report_id"), result.foreignKeys.single().columns)
			assertEquals(false, result.columns.first { it.name == "payamak_report_id" }.nullable)
		} finally {
			StandardServiceRegistryBuilder.destroy(registry)
		}
	}
}
