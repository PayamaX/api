package no1.payamax.api.oauth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.util.AntPathMatcher

class OpenApiGroupsTest {
	private val groups = OpenApiGroups()
	private val matcher = AntPathMatcher()

	@Test
	fun `managers document lists protected endpoints`() {
		val managers = groups.managersOpenApi()

		assertEquals("Managers", managers.displayName)
		assertTrue(includes(managers, "/v1/Protected/origins"))
		assertFalse(includes(managers, "/v1/Public/origins"))
		assertFalse(includes(managers, "/origins"))
	}

	@Test
	fun `users document lists public endpoints`() {
		val users = groups.usersOpenApi()

		assertEquals("Users", users.displayName)
		assertTrue(includes(users, "/v1/Public/origins"))
		assertFalse(includes(users, "/v1/Protected/origins"))
		assertFalse(includes(users, "/origins"))
	}

	@Test
	fun `developers document lists every other endpoint`() {
		val developers = groups.developersOpenApi()

		assertEquals("Developers", developers.displayName)
		assertTrue(includes(developers, "/origins"))
		assertTrue(includes(developers, "/v1/internal/origins"))
		assertFalse(includes(developers, "/v1/Protected/origins"))
		assertFalse(includes(developers, "/v1/Public/origins"))
	}

	private fun includes(group: GroupedOpenApi, path: String): Boolean {
		val pathsToMatch = group.pathsToMatch.orEmpty()
		val pathsToExclude = group.pathsToExclude.orEmpty()
		val included = pathsToMatch.isEmpty() || pathsToMatch.any { matcher.match(it, path) }
		val excluded = pathsToExclude.any { matcher.match(it, path) }
		return included && !excluded
	}
}
