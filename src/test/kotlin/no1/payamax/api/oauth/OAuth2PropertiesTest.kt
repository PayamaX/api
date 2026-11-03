package no1.payamax.api.oauth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class OAuth2PropertiesTest {
	@Test
	fun `uses the public realm url when no issuers are configured`() {
		val properties = configured().apply { issuers = emptyList() }

		assertEquals(setOf("https://kc:8080/realms/pax"), properties.acceptedIssuers())
	}

	@Test
	fun `uses configured issuers instead of the public realm url`() {
		val properties = configured().apply {
			issuers = listOf(" https://auth.example/realms/pax/ ", "")
		}

		assertEquals(setOf("https://auth.example/realms/pax"), properties.acceptedIssuers())
	}

	@Test
	fun `accepts configured audiences and the client id`() {
		val properties = configured()

		assertEquals(setOf("SwaggerUI", "account"), properties.acceptedAudiences())
	}

	@Test
	fun `rejects insecure metadata when https is required`() {
		val properties = configured().apply {
			requiresSecureMetadata = true
			privateRealmUrl = "http://kckc:8080/realms/pax"
		}

		assertThrows(IllegalArgumentException::class.java) { properties.validate() }
	}

	private fun configured(): OAuth2Properties {
		return OAuth2Properties().apply {
			realm = "pax"
			clientId = "SwaggerUI"
			publicRealmUrl = "https://kc:8080/realms/pax/"
			privateRealmUrl = "http://kckc:8080/realms/pax"
			healthUrl = "http://kckc:8080/health/ready"
			requiresSecureMetadata = false
			audiences = listOf("account")
		}
	}
}
