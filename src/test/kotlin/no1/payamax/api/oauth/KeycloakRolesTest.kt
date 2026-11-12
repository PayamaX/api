package no1.payamax.api.oauth

import no1.payamax.api.config.oauth.keycloakRoles
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KeycloakRolesTest {
	@Test
	fun `uses roles from the login client when that client is an accepted audience`() {
		// Arrange
		val claims = mapOf(
			"azp" to "Admin",
			"realm_access" to mapOf("roles" to listOf("default-roles-pax")),
			"resource_access" to mapOf(
				"Admin" to mapOf("roles" to listOf("ADMIN")),
				"SwaggerUI" to mapOf("roles" to listOf("Developer")),
			),
		)

		// Act
		val roles = keycloakRoles(claims, clientId = "SwaggerUI", audiences = setOf("Admin", "SwaggerUI", "Android"))

		// Assert
		assertEquals(setOf("default-roles-pax", "ADMIN"), roles)
	}

	@Test
	fun `uses the configured client when the login client is not an accepted audience`() {
		// Arrange
		val claims = mapOf(
			"azp" to "account",
			"resource_access" to mapOf(
				"account" to mapOf("roles" to listOf("manage-account")),
				"SwaggerUI" to mapOf("roles" to listOf("Developer")),
			),
		)

		// Act
		val roles = keycloakRoles(claims, clientId = "SwaggerUI", audiences = setOf("Admin", "SwaggerUI", "Android"))

		// Assert
		assertEquals(setOf("Developer"), roles)
	}
}
