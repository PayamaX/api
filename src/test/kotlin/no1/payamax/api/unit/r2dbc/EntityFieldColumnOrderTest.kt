package no1.payamax.api.unit.r2dbc

import no1.payamax.api.config.r2dbc.EntityFieldColumnOrderingStrategy
import no1.payamax.api.config.r2dbc.EntitySuffixPhysicalNamingStrategy
import no1.payamax.api.config.r2dbc.ReadableForeignKeyNamingStrategy
import no1.payamax.api.modules.payamakreport.PayamakReportEntity
import no1.payamax.api.modules.payamakreport.payamakreportruleprocessresult.PayamakReportRuleProcessResultEntity
import no1.payamax.api.modules.rankedorigin.RankedOriginEntity
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.boot.spi.MetadataImplementor
import org.hibernate.cfg.AvailableSettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EntityFieldColumnOrderTest {
	@Test
	fun `create table columns follow entity field order`() {
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
			(metadata as MetadataImplementor).orderColumns(true)
			assertEquals(
				listOf(
					"id",
					"global_id",
					"updated",
					"deleted",
					"origin",
					"rank",
					"owner_global_id",
					"position",
					"comment",
				),
				columns(metadata, RankedOriginEntity::class.java),
			)
			assertEquals(
				listOf(
					"id",
					"global_id",
					"state",
					"origin",
					"body",
					"sha512",
					"reporter_user_global_id",
					"reporter_comment",
					"reviewer_comment",
					"flagged",
					"detected_diagnostics",
					"expected_diagnostics",
					"decided_diagnostics",
				),
				columns(metadata, PayamakReportEntity::class.java),
			)
			assertEquals(
				listOf(
					"id",
					"global_id",
					"payamak_report_id",
					"processor_name",
					"processor_version",
					"general_rank",
					"user_override_rank",
					"processor_provided_comment",
					"position",
				),
				columns(metadata, PayamakReportRuleProcessResultEntity::class.java),
			)
		} finally {
			StandardServiceRegistryBuilder.destroy(registry)
		}
	}

	private fun columns(metadata: org.hibernate.boot.Metadata, type: Class<*>): List<String> =
		metadata.entityBindings.first { it.mappedClass == type }.table.columns.map { it.canonicalName }
}
