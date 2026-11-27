package no1.payamax.api.integration.modules.rankedorigin

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import kotlin.test.assertEquals

class DeleteProtectedRankedOriginsTest : RankedOriginsProtectedV1TestBase() {
	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `delete removes only the listed origins`() {
		// Arrange
		val (titleId, numberId) = runBlocking {
			insert("PayamaX", position = 1, rank = 1.0, owner = null)
			insert("09121234567", position = 2, rank = 0.5, owner = null)
			insert("Alerts", position = 3, rank = 0.25, owner = null)
			storedGlobalId("PayamaX") to storedGlobalId("09121234567")
		}
		val missing = UUID.randomUUID()

		// Act
		val response = deleteOrigins(listOf(UUID.fromString(titleId), UUID.fromString(numberId), missing))

		// Assert
		response
			.andExpect(status().isOk)
			.andExpect(content().string(""))
		runBlocking {
			assertEquals(true, isDeleted("PayamaX"))
			assertEquals(true, isDeleted("09121234567"))
			assertEquals(1L, countOrigins("Alerts"))
			assertEquals(false, isDeleted("Alerts"))
		}
	}

	@Test
	@WithMockUser(roles = ["USER"])
	fun `delete of listed origins rejects a caller without ROLE_ADMIN`() {
		// Arrange
		val titleId = runBlocking {
			insert("PayamaX", position = 1, rank = 1.0, owner = null)
			UUID.fromString(storedGlobalId("PayamaX"))
		}

		// Act
		val response = deleteOrigins(listOf(titleId))

		// Assert
		response.expectForbidden()
		runBlocking { assertEquals(1L, countOrigins("PayamaX")) }
	}

	@Test
	fun `delete of listed origins rejects a caller with no credentials`() {
		// Arrange
		val titleId = runBlocking {
			insert("PayamaX", position = 1, rank = 1.0, owner = null)
			UUID.fromString(storedGlobalId("PayamaX"))
		}

		// Act
		val response = deleteOrigins(listOf(titleId))

		// Assert
		response.expectForbidden()
		runBlocking { assertEquals(1L, countOrigins("PayamaX")) }
	}

	@Test
	fun `delete of listed origins rejects an untrusted audience`() {
		// Arrange
		val token = untrustedToken()
		val titleId = runBlocking {
			insert("PayamaX", position = 1, rank = 1.0, owner = null)
			UUID.fromString(storedGlobalId("PayamaX"))
		}

		// Act
		val response = deleteOrigins(listOf(titleId), token = token)

		// Assert
		response.andExpect(status().isUnauthorized)
		runBlocking { assertEquals(1L, countOrigins("PayamaX")) }
	}

	@Test
	fun `delete of listed origins rejects a malformed token`() {
		// Arrange
		val titleId = runBlocking {
			insert("PayamaX", position = 1, rank = 1.0, owner = null)
			UUID.fromString(storedGlobalId("PayamaX"))
		}

		// Act
		val response = deleteOrigins(listOf(titleId), token = "not-a-jwt")

		// Assert
		response.andExpect(status().isUnauthorized)
		runBlocking { assertEquals(1L, countOrigins("PayamaX")) }
	}
}
