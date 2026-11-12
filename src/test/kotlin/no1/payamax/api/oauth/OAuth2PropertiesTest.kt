package no1.payamax.api.oauth

import no1.payamax.api.config.oauth.OAuth2Properties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OAuth2PropertiesTest {
	@Test
	fun `uses the public realm url when no issuers are configured`() {
		// Arrange
		val properties = configured().apply { issuers = emptyList() }

		// Act
		val issuers = properties.acceptedIssuers()

		// Assert
		assertEquals(setOf("https://kc:8080/realms/pax"), issuers)
	}

	@Test
	fun `uses configured issuers instead of the public realm url`() {
		// Arrange
		val properties = configured().apply {
			issuers = listOf(" https://auth.example/realms/pax/ ", "")
		}

		// Act
		val issuers = properties.acceptedIssuers()

		// Assert
		assertEquals(setOf("https://auth.example/realms/pax"), issuers)
	}

	@Test
	fun `builds the identity provider end session url from the public realm`() {
		// Arrange
		val properties = configured().apply { clientId = "Swagger UI" }

		// Act
		val endSessionUrl = properties.endSessionUrl()

		// Assert
		assertEquals(
			"https://kc:8080/realms/pax/protocol/openid-connect/logout?client_id=Swagger+UI",
			endSessionUrl,
		)
	}

	@Test
	fun `accepts configured audiences and the client id`() {
		// Arrange
		val properties = configured()

		// Act
		val audiences = properties.acceptedAudiences()

		// Assert
		assertEquals(setOf("SwaggerUI", "account"), audiences)
	}

	@Test
	fun `rejects insecure metadata when https is required`() {
		// Arrange
		val properties = configured().apply {
			requiresSecureMetadata = true
			privateRealmUrl = "http://kckc:8080/realms/pax"
		}

		// Act
		val error = assertThrows(IllegalArgumentException::class.java) { properties.validate() }

		// Assert
		assertTrue(error.message!!.contains("oauth2.private-realm-url must use HTTPS"))
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
