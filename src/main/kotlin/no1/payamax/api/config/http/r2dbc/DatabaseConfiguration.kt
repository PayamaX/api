package no1.payamax.api.config.http.r2dbc

import org.reactivestreams.Publisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.CustomConversions
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions
import org.springframework.data.r2dbc.dialect.DialectResolver
import org.springframework.stereotype.Component
import org.springframework.data.relational.core.mapping.NamingStrategy
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback
import org.springframework.data.relational.core.sql.SqlIdentifier
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@Configuration
class DatabaseConfiguration {
	@Bean
	fun namingStrategy(): NamingStrategy = object : NamingStrategy {
		override fun getTableName(type: Class<*>): String = super.getTableName(type).removeSuffix("_entity")
	}

	@Bean
	fun r2dbcCustomConversions(databaseClient: DatabaseClient): R2dbcCustomConversions {
		val dialect = DialectResolver.getDialect(databaseClient.connectionFactory)
		val storeConverters = ArrayList<Any>(dialect.converters.size + R2dbcCustomConversions.STORE_CONVERTERS.size)
		storeConverters.addAll(dialect.converters)
		storeConverters.addAll(R2dbcCustomConversions.STORE_CONVERTERS)
		return R2dbcCustomConversions(
			CustomConversions.StoreConversions.of(dialect.simpleTypeHolder, storeConverters),
			kotlinInstantConverters,
		)
	}
}

internal val kotlinInstantConverters: List<Converter<*, *>> = listOf(
	KotlinInstantToLocalDateTime(),
	LocalDateTimeToKotlinInstant(),
)

private class KotlinInstantToLocalDateTime : Converter<Instant, LocalDateTime> {
	override fun convert(source: Instant): LocalDateTime =
		LocalDateTime.ofInstant(source.toJavaInstant(), ZoneOffset.UTC)
}

private class LocalDateTimeToKotlinInstant : Converter<LocalDateTime, Instant> {
	override fun convert(source: LocalDateTime): Instant =
		source.toInstant(ZoneOffset.UTC).toKotlinInstant()
}
