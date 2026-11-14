package no1.payamax.api.modules.payamakreport

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import kotlin.test.assertEquals

class PayamakReportProtectedV1PutTest : PayamakReportProtectedV1TestBase() {
	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `put stores the decision and marks the report reviewed`() {
		// Arrange
		val globalId = runBlocking { insert(origin = "PayamaX", state = "POSTPONED", flagged = true) }

		// Act
		val response = review(globalId, decidedDiagnostics = "Useful", reviewerComment = "kept")

		// Assert
		response
			.expectReport(globalId, state = "REVIEWED", flagged = true, reviewerComment = "kept", decidedDiagnostics = "Useful")
			.andExpect(jsonPath("$.origin").value("PayamaX"))
		runBlocking {
			assertEquals(
				StoredReport("PayamaX", "REVIEWED", true, "kept", "Useful"),
				stored(globalId),
			)
		}
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `put clears the reviewer comment when it is null`() {
		// Arrange
		val globalId = runBlocking {
			insert(state = "PENDING", reviewerComment = "old note", decidedDiagnostics = "Spam")
		}

		// Act
		val response = review(globalId, decidedDiagnostics = "Important", reviewerComment = null)

		// Assert
		response.expectReport(globalId, state = "REVIEWED", flagged = false, reviewerComment = null, decidedDiagnostics = "Important")
		runBlocking {
			assertEquals(
				StoredReport("PayamaX", "REVIEWED", false, null, "Important"),
				stored(globalId),
			)
		}
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `put returns bad request when the report is missing`() {
		// Arrange
		val missing = UUID.randomUUID()

		// Act
		val response = review(missing, decidedDiagnostics = "Spam", reviewerComment = "missing")

		// Assert
		response.expectMissing("$REPORTS/$missing")
	}

	@Test
	@WithMockUser(roles = ["USER"])
	fun `put rejects a caller without ROLE_ADMIN`() {
		// Arrange
		val globalId = UUID.randomUUID()

		// Act
		val response = review(globalId, decidedDiagnostics = "Spam", reviewerComment = null)

		// Assert
		response.expectForbidden()
	}

	@Test
	fun `put rejects a caller with no credentials`() {
		// Arrange
		val globalId = UUID.randomUUID()

		// Act
		val response = review(globalId, decidedDiagnostics = "Spam", reviewerComment = null)

		// Assert
		response.expectForbidden()
	}

	@Test
	fun `put rejects an untrusted audience`() {
		// Arrange
		val token = untrustedToken()

		// Act
		val response = review(UUID.randomUUID(), decidedDiagnostics = "Spam", reviewerComment = null, token = token)

		// Assert
		response.andExpect(status().isUnauthorized)
	}

	@Test
	fun `put rejects a malformed token`() {
		// Arrange
		val token = "not-a-jwt"

		// Act
		val response = review(UUID.randomUUID(), decidedDiagnostics = "Spam", reviewerComment = null, token = token)

		// Assert
		response.andExpect(status().isUnauthorized)
	}

	private fun review(globalId: UUID, decidedDiagnostics: String, reviewerComment: String?, token: String? = null): ResultActions {
		val comment = if (reviewerComment == null) "null" else "\"$reviewerComment\""
		val request = put("$REPORTS/$globalId")
			.contentType(MediaType.APPLICATION_JSON)
			.accept(MediaType.APPLICATION_JSON)
			.content("""{"decidedDiagnostics":"$decidedDiagnostics","reviewerComment":$comment}""")
		return exchange(withToken(request, token))
	}
}
