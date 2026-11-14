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

class PayamakReportProtectedV1FlagTest : PayamakReportProtectedV1TestBase() {
	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `flag marks a report`() {
		// Arrange
		val globalId = runBlocking { insert(state = "PENDING", flagged = false) }

		// Act
		val response = flag(globalId, flagged = true)

		// Assert
		response.expectReport(globalId, state = "PENDING", flagged = true)
		runBlocking { assertEquals(true, stored(globalId).flagged) }
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `flag clears a report flag`() {
		// Arrange
		val globalId = runBlocking { insert(state = "REVIEWED", flagged = true, decidedDiagnostics = "Useful") }

		// Act
		val response = flag(globalId, flagged = false)

		// Assert
		response.expectReport(globalId, state = "REVIEWED", flagged = false, decidedDiagnostics = "Useful")
		runBlocking {
			assertEquals(
				StoredReport("PayamaX", "REVIEWED", false, null, "Useful"),
				stored(globalId),
			)
		}
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `flag returns bad request when the report is missing`() {
		// Arrange
		val missing = UUID.randomUUID()

		// Act
		val response = flag(missing, flagged = true)

		// Assert
		response.expectMissing("$REPORTS/$missing/flag")
	}

	@Test
	@WithMockUser(roles = ["USER"])
	fun `flag rejects a caller without ROLE_ADMIN`() {
		// Arrange
		val globalId = UUID.randomUUID()

		// Act
		val response = flag(globalId, flagged = true)

		// Assert
		response.expectForbidden()
	}

	@Test
	fun `flag rejects a caller with no credentials`() {
		// Arrange
		// No token and no authenticated test user.

		// Act
		val response = flag(UUID.randomUUID(), flagged = true)

		// Assert
		response.expectForbidden()
	}

	@Test
	fun `flag rejects an untrusted audience`() {
		// Arrange
		val token = untrustedToken()

		// Act
		val response = flag(UUID.randomUUID(), flagged = true, token = token)

		// Assert
		response.andExpect(status().isUnauthorized)
	}

	@Test
	fun `flag rejects a malformed token`() {
		// Arrange
		val token = "not-a-jwt"

		// Act
		val response = flag(UUID.randomUUID(), flagged = false, token = token)

		// Assert
		response.andExpect(status().isUnauthorized)
	}

	private fun flag(globalId: UUID, flagged: Boolean, token: String? = null): ResultActions {
		val request = patch("$REPORTS/$globalId/flag")
			.param("flagged", flagged.toString())
			.accept(MediaType.APPLICATION_JSON)
		return exchange(withToken(request, token))
	}
}
