package no1.payamax.api.unit.r2dbc

import no1.libs.dbmigration.databaseNullable
import no1.payamax.api.modules.payamakreport.PayamakReportEntity
import no1.payamax.api.modules.payamakreport.payamakreportruleprocessresult.PayamakReportRuleProcessResultEntity
import no1.payamax.api.modules.rankedorigin.RankedOriginEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.full.memberProperties

class DatabaseNullabilityTest {
	@Test
	fun `database nullability follows the kotlin type except id`() {
		val entities = listOf(
			RankedOriginEntity::class,
			PayamakReportEntity::class,
			PayamakReportRuleProcessResultEntity::class,
		)
		for (entity in entities) {
			for (property in entity.memberProperties) {
				val nullable = databaseNullable(entity.java, property.name)
				val expected = property.name != "id" && property.returnType.isMarkedNullable
				assertEquals(expected, nullable, "${entity.simpleName}.${property.name}")
			}
		}
	}
}
