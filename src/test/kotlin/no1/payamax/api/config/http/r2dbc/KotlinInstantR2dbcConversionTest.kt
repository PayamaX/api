package no1.payamax.api.config.http.r2dbc

import no1.payamax.api.config.r2dbc.kotlinInstantConverters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.core.convert.support.GenericConversionService
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions
import org.springframework.data.r2dbc.dialect.PostgresDialect
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.Instant

class KotlinInstantR2dbcConversionTest {
	@Test
	fun `a kotlin instant is stored as utc local date time`() {
		val conversions = R2dbcCustomConversions.of(PostgresDialect.INSTANCE, kotlinInstantConverters)
		val service = GenericConversionService()
		conversions.registerConvertersIn(service)
		val instant = Instant.ofEpochSecond(2, 234_567_890)

		val stored = service.convert(instant, LocalDateTime::class.java)

		assertEquals(LocalDateTime.ofInstant(instant, ZoneOffset.UTC), stored)
		assertEquals(instant, service.convert(stored, Instant::class.java))
	}
}
