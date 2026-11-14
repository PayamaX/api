package no1.payamax.api.modules.payamakreport

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import kotlin.test.assertEquals

class PayamakReportProtectedV1PostponeTest : PayamakReportProtectedV1TestBase() {
	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `postpone marks a pending report postponed`() {
		// Arrange
		val globalId = runBlocking { insert(state = "PENDING", flagged = false) }

		// Act
		val response = postpone(globalId)

		// Assert
		response.expectReport(globalId, state = "POSTPONED", flagged = false)
		runBlocking { assertEquals("POSTPONED", stored(globalId).state) }
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `postpone leaves a postponed report unchanged`() {
		// Arrange
		val globalId = runBlocking { insert(state = "POSTPONED", flagged = true, reviewerComment = "waiting") }

		// Act
		val response = postpone(globalId)

		// Assert
		response.expectReport(globalId, state = "POSTPONED", flagged = true, reviewerComment = "waiting")
		runBlocking {
			assertEquals(
				StoredReport("PayamaX", "POSTPONED", true, "waiting", null),
				stored(globalId),
			)
		}
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `postpone returns bad request when the report is missing`() {
		// Arrange
		val missing = UUID.randomUUID()

		// Act
		val response = postpone(missing)

		// Assert
		response.expectMissing("$REPORTS/$missing/postpone")
	}

	@Test
	@WithMockUser(roles = ["USER"])
	fun `postpone rejects a caller without ROLE_ADMIN`() {
		// Arrange
		// The caller is authenticated, without the admin role.

		// Act
		val response = postpone(UUID.randomUUID())

		// Assert
		response.expectForbidden()
	}

	@Test
	fun `postpone rejects a caller with no credentials`() {
		// Arrange
		// No token and no authenticated test user.

		// Act
		val response = postpone(UUID.randomUUID())

		// Assert
		response.expectForbidden()
	}

	@Test
	fun `postpone rejects an untrusted audience`() {
		// Arrange
		val token = untrustedToken()

		// Act
		val response = postpone(UUID.randomUUID(), token = token)

		// Assert
		response.andExpect(status().isUnauthorized)
	}

	@Test
	fun `postpone rejects a malformed token`() {
		// Arrange
		val token = "not-a-jwt"

		// Act
		val response = postpone(UUID.randomUUID(), token = token)

		// Assert
		response.andExpect(status().isUnauthorized)
	}

	private fun postpone(globalId: UUID, token: String? = null): ResultActions {
		val request = patch("$REPORTS/$globalId/postpone").accept(MediaType.APPLICATION_JSON)
		return exchange(withToken(request, token))
	}
}
