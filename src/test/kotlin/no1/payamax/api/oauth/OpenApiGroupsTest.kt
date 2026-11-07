package no1.payamax.api.oauth

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import no1.payamax.api.config.http.oauth.OpenApiGroups
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springdoc.core.models.GroupedOpenApi

class OpenApiGroupsTest {
	private val groups = OpenApiGroups()
	private val paths = listOf(
		"/v1/Protected/origins",
		"/v1/protected/accounts",
		"/v1/PUBLIC/profile",
		"/v1/PublicExtra",
		"/origins",
	)

	@Test
	fun `protected document lists protected endpoints ignoring case`() {
		// Arrange
		val group = groups.protectedOpenApi()

		// Act
		val documented = pathsIn(group)

		// Assert
		assertEquals(setOf("/v1/Protected/origins", "/v1/protected/accounts"), documented)
	}

	@Test
	fun `public document lists public endpoints ignoring case`() {
		// Arrange
		val group = groups.publicOpenApi()

		// Act
		val documented = pathsIn(group)

		// Assert
		assertEquals(setOf("/v1/PUBLIC/profile"), documented)
	}

	@Test
	fun `private document lists every other endpoint`() {
		// Arrange
		val group = groups.privateOpenApi()

		// Act
		val documented = pathsIn(group)

		// Assert
		assertEquals(setOf("/v1/PublicExtra", "/origins"), documented)
	}

	private fun pathsIn(group: GroupedOpenApi): Set<String> {
		val openApi = OpenAPI().paths(Paths())
		paths.forEach { openApi.paths.addPathItem(it, PathItem()) }
		group.openApiCustomizers.forEach { it.customise(openApi) }
		return openApi.paths.keys.toSet()
	}
}
