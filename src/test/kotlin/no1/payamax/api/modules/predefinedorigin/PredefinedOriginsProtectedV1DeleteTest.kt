package no1.payamax.api.modules.predefinedorigin

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import kotlin.test.assertEquals

class PredefinedOriginsProtectedV1DeleteTest : PredefinedOriginsProtectedV1TestBase() {
	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `delete removes an origin`() {
		// Arrange
		val titleId = runBlocking {
			insert("PayamaX", position = 1, rank = 1.0, owner = null)
			insert("09121234567", position = 2, rank = 0.5, owner = null)
			UUID.fromString(storedGlobalId("PayamaX"))
		}

		// Act
		val response = deleteOrigin(titleId)

		// Assert
		response.andExpect(status().isOk)
		runBlocking {
			assertEquals(0L, countOrigins("PayamaX"))
			assertEquals(1L, countOrigins("09121234567"))
		}
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `delete of an unknown origin is a bad request`() {
		// Arrange
		val missing = UUID.randomUUID()

		// Act
		val response = deleteOrigin(missing)

		// Assert
		response
			.andExpect(status().isBadRequest)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.title").value("Bad Request"))
			.andExpect(jsonPath("$.detail").value("Entity not found"))
	}

	@Test
	@WithMockUser(roles = ["USER"])
	fun `delete rejects a caller without ROLE_ADMIN`() {
		// Arrange
		val titleId = runBlocking {
			insert("PayamaX", position = 1, rank = 1.0, owner = null)
			UUID.fromString(storedGlobalId("PayamaX"))
		}

		// Act
		val response = deleteOrigin(titleId)

		// Assert
		response.expectForbidden()
		runBlocking { assertEquals(1L, countOrigins("PayamaX")) }
	}

	@Test
	fun `delete rejects a caller with no credentials`() {
		// Arrange
		val titleId = runBlocking {
			insert("PayamaX", position = 1, rank = 1.0, owner = null)
			UUID.fromString(storedGlobalId("PayamaX"))
		}

		// Act
		val response = deleteOrigin(titleId)

		// Assert
		response.expectForbidden()
		runBlocking { assertEquals(1L, countOrigins("PayamaX")) }
	}

	@Test
	fun `delete rejects an untrusted audience`() {
		// Arrange
		val token = untrustedToken()
		val titleId = runBlocking {
			insert("PayamaX", position = 1, rank = 1.0, owner = null)
			UUID.fromString(storedGlobalId("PayamaX"))
		}

		// Act
		val response = deleteOrigin(titleId, token = token)

		// Assert
		response.andExpect(status().isUnauthorized)
		runBlocking { assertEquals(1L, countOrigins("PayamaX")) }
	}

	@Test
	fun `delete rejects a malformed token`() {
		// Arrange
		val titleId = runBlocking {
			insert("PayamaX", position = 1, rank = 1.0, owner = null)
			UUID.fromString(storedGlobalId("PayamaX"))
		}

		// Act
		val response = deleteOrigin(titleId, token = "not-a-jwt")

		// Assert
		response.andExpect(status().isUnauthorized)
		runBlocking { assertEquals(1L, countOrigins("PayamaX")) }
	}
}
