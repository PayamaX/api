package no1.payamax.api.config.http.json

import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverters
import no1.payamax.api.config.json.KotlinInstantJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springdoc.core.converters.AdditionalModelsConverter
import org.springdoc.core.properties.SpringDocConfigProperties
import org.springdoc.core.providers.ObjectMapperProvider
import tools.jackson.databind.DatabindException
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import java.time.Instant

class KotlinInstantJsonTest {
	private val mapper = JsonMapper.builder().addModule(
		SimpleModule()
			.addSerializer(Instant::class.java, KotlinInstantJson.Serializer())
			.addDeserializer(Instant::class.java, KotlinInstantJson.Deserializer()),
	).build()

	@Test
	fun `an instant is a seconds and nanos string`() {
		val instant = Instant.ofEpochSecond(2, 234_567_890)

		val json = mapper.writeValueAsString(instant)

		assertEquals("\"2.234567890\"", json)
		assertEquals(instant, mapper.readValue(json, Instant::class.java))
	}

	@Test
	fun `a negative instant keeps nanoseconds`() {
		val instant = Instant.ofEpochSecond(-1, 998_500_000)

		val json = mapper.writeValueAsString(instant)

		assertEquals("\"-1.998500000\"", json)
		assertEquals(instant, mapper.readValue(json, Instant::class.java))
	}

	@Test
	fun `a field is one value`() {
		val instant = Instant.ofEpochSecond(1, 1)

		val json = mapper.writeValueAsString(Sample(instant))

		assertEquals("""{"at":"1.000000001"}""", json)
	}

	@Test
	fun `openapi describes an instant as one string`() {
		KotlinInstantJson.registerOpenApi()
		val converters = ModelConverters()
		converters.addConverter(AdditionalModelsConverter(ObjectMapperProvider(SpringDocConfigProperties())))

		val schema = converters.resolveAsResolvedSchema(AnnotatedType(Instant::class.java)).schema

		assertTrue("string" in (schema.types ?: setOf(schema.type)))
		assertNull(schema.properties)
	}

	@Test
	fun `an object instant is rejected`() {
		assertThrows(DatabindException::class.java) {
			mapper.readValue("""{"epochSeconds":1,"nanosecondsOfSecond":2}""", Instant::class.java)
		}
	}

	private data class Sample(val at: Instant)
}
