package no1.payamax.api.config.http.json

import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import kotlin.time.Instant

@Component
class KotlinInstantConverter : Converter<String, Instant> {
	override fun convert(source: String): Instant =
		KotlinInstantJson.parse(source)
			?: throw IllegalArgumentException("Instant must use the millis.nanos form, such as 2234.567890")
}
