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
				return ctxt.reportInputMismatch(Instant::class.java, "Instant must be a seconds.nanos string")
			}
			return parse(parser.valueAsString)
				?: ctxt.reportInputMismatch(Instant::class.java, "Instant must use the seconds.nanos form, such as 2234.567890")
		}
	}

	internal companion object {
		private val TEXT = Regex("""(-|\\+)?(0|[1-9]\d*)\.(\d{9})""")

		fun registerOpenApi() {
			SpringDocUtils.getConfig()
				.addSimpleTypesForParameterObject(Instant::class.java)
				.replaceWithSchema(Instant::class.java, openApiSchema())
		}

		fun openApiSchema(): StringSchema = StringSchema().apply {
			pattern = "^${TEXT.pattern}$"
			example = format(Instant.now())
			description = "Epoch seconds, a dot, then the remaining nanoseconds."
		}

		fun format(instant: Instant): String {
			return "${instant.epochSecond}.${"%09d".format(instant.nano)}"
		}

		fun parse(text: String): Instant? {
			val match = TEXT.matchEntire(text) ?: return null
			val parts = text.split('.')
			if(parts.size != 2) return null
			val seconds =parts[0].toLongOrNull() ?: return null
			val nanos = parts[1].toLongOrNull() ?: return null
			return Instant.ofEpochSecond(seconds, nanos)
		}

		private fun parts(seconds: String, nanos: Int): Pair<Long, Int> {
			return Pair(seconds.toLong(), nanos)
		}
	}
}
