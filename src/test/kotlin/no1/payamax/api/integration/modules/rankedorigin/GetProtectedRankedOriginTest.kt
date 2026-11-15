package no1.payamax.api.integration.modules.rankedorigin

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

class GetProtectedRankedOriginTest : RankedOriginsProtectedV1TestBase() {
	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `get by global id returns that origin`() {
		// Arrange
		val owner = UUID.randomUUID()
		val globalId = runBlocking {
			insert("PayamaX", position = 1, rank = 0.25, owner = null)
			insert("09121234567", position = 2, rank = 0.75, owner = owner)
			UUID.fromString(storedGlobalId("09121234567"))
		}

		// Act
		val response = getOrigin(globalId.toString())

		// Assert
		response
			.andExpect(status().isOk)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.globalId").value(globalId.toString()))
			.andExpect(jsonPath("$.origin").value("09121234567"))
			.andExpect(jsonPath("$.rank").value(0.75))
			.andExpect(jsonPath("$.position").value(2))
			.andExpect(jsonPath("$.ownerGlobalId").value(owner.toString()))
			.andExpect(jsonPath("$.localId").isNumber())
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `get by origin returns that origin`() {
		// Arrange
		val owner = UUID.randomUUID()
		val globalId = runBlocking {
			insert("PayamaX", position = 1, rank = 0.25, owner = null)
			insert("09121234567", position = 2, rank = 0.75, owner = owner)
			storedGlobalId("09121234567")
		}

		// Act
		val response = getOrigin("09121234567")

		// Assert
		response
			.andExpect(status().isOk)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.globalId").value(globalId))
			.andExpect(jsonPath("$.origin").value("09121234567"))
			.andExpect(jsonPath("$.rank").value(0.75))
			.andExpect(jsonPath("$.position").value(2))
			.andExpect(jsonPath("$.ownerGlobalId").value(owner.toString()))
			.andExpect(jsonPath("$.localId").isNumber())
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `get by global id returns bad request when the origin is missing`() {
		// Arrange
		val missing = UUID.randomUUID()

		// Act
		val response = getOrigin(missing.toString())

		// Assert
		response
			.andExpect(status().isNoContent)
			.andExpect(content().string(""))
	}

	@Test
	@WithMockUser(roles = ["USER"])
	fun `get by global id rejects a caller without ROLE_ADMIN`() {
		// Arrange
		val globalId = UUID.randomUUID()

		// Act
		val response = getOrigin(globalId.toString())

		// Assert
		response.expectForbidden()
	}

	@Test
	fun `get by global id rejects a caller with no credentials`() {
		// Arrange
		val globalId = UUID.randomUUID()

		// Act
		val response = getOrigin(globalId.toString())

		// Assert
		response.expectForbidden()
	}

	@Test
	fun `get by global id rejects an untrusted audience`() {
		// Arrange
		val token = untrustedToken()
		val globalId = UUID.randomUUID()

		// Act
		val response = getOrigin(globalId.toString(), token = token)

		// Assert
		response.andExpect(status().isUnauthorized)
	}

	@Test
	fun `get by global id rejects a malformed token`() {
		// Arrange
		val globalId = UUID.randomUUID()

		// Act
		val response = getOrigin(globalId.toString(), token = "not-a-jwt")

		// Assert
		response.andExpect(status().isUnauthorized)
	}
}
