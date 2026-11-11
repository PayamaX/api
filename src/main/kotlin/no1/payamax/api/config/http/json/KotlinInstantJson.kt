package no1.payamax.api.config.http.json

import io.swagger.v3.oas.models.media.StringSchema
import org.springdoc.core.utils.SpringDocUtils
import org.springframework.boot.jackson.JacksonComponent
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.core.JsonToken
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
import java.time.Instant

/**
 * JSON form of [Instant]: epoch milliseconds, a dot, then the remaining nanoseconds
 * of that millisecond as six digits. `2234.567890` is 2234 ms and 567890 ns.
 */
@JacksonComponent
class KotlinInstantJson {
	init {
		registerOpenApi()
	}

	class Serializer : ValueSerializer<Instant>() {
		override fun serialize(value: Instant, gen: JsonGenerator, ctxt: SerializationContext) {
			gen.writeString(format(value))
		}
	}

	class Deserializer : ValueDeserializer<Instant>() {
		override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): Instant {
			if (parser.currentToken() != JsonToken.VALUE_STRING) {
				return ctxt.reportInputMismatch(Instant::class.java, "Instant must be a millis.nanos string")
			}
			return parse(parser.getText())
				?: ctxt.reportInputMismatch(Instant::class.java, "Instant must use the millis.nanos form, such as 2234.567890")
		}
	}

	internal companion object {
		private val TEXT = Regex("""(-?)(0|[1-9]\d*)\.(\d{0,9})""")

		fun registerOpenApi() {
			SpringDocUtils.getConfig()
				.addSimpleTypesForParameterObject(Instant::class.java)
				.replaceWithSchema(Instant::class.java, openApiSchema())
		}

		fun openApiSchema(): StringSchema = StringSchema().apply {
			pattern = "^${TEXT.pattern}$"
			example = "2234.567890"
			description = "Epoch milliseconds, a dot, then six digits of the remaining nanoseconds."
		}

		fun format(instant: Instant): String {
			val nanos = instant.nano.toLong()
			if (instant.epochSecond >= 0) {
				return text(instant.epochSecond, nanos)
			}
			val seconds = if (nanos == 0L) -instant.epochSecond else -instant.epochSecond - 1
			val remainder = if (nanos == 0L) 0L else 1_000_000_000 - nanos
			return "-${text(seconds, remainder)}"
		}

		fun parse(text: String): Instant? {
			val match = TEXT.matchEntire(text) ?: return null
			val negative = match.groupValues[1].isNotEmpty()
			val (seconds, nanos) = parts(match.groupValues[2], match.groupValues[3].toInt())
			return try {
				when {
					!negative -> Instant.ofEpochSecond(seconds, nanos.toLong())
					nanos == 0 -> Instant.ofEpochSecond(-seconds, 0)
					else -> Instant.ofEpochSecond(-(seconds + 1), (1_000_000_000 - nanos).toLong())
				}
			} catch (_: IllegalArgumentException) {
				null
			}
		}

		private fun text(seconds: Long, nanosecondsOfSecond: Long): String {
			val millisInSecond = nanosecondsOfSecond / 1_000_000
			val fraction = nanosecondsOfSecond % 1_000_000
			val millis = if (seconds == 0L) {
				millisInSecond.toString()
			} else {
				seconds.toString() + "%03d".format(millisInSecond)
			}
			return "$millis.${"%06d".format(fraction)}"
		}

		private fun parts(millis: String, fraction: Int): Pair<Long, Int> {
			val seconds: Long
			val millisInSecond: Int
			if (millis.length <= 3) {
				seconds = 0
				millisInSecond = millis.toInt()
			} else {
				seconds = millis.dropLast(3).toLong()
				millisInSecond = millis.takeLast(3).toInt()
			}
			return seconds to millisInSecond * 1_000_000 + fraction
		}
	}
}
