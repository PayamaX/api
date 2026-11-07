package no1.payamax.api.modules.predefinedorigin

import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import kotlin.test.assertEquals

class PredefinedOriginsProtectedV1PostTest : PredefinedOriginsProtectedV1TestBase() {
	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `post creates an origin`() {
		// Arrange
		val body = originJson(origin = "Alerts", rank = 0.5, position = 4)

		// Act
		val saved = submit(post(ORIGINS), body)

		// Assert
		val storedId = runBlocking { storedGlobalId("Alerts") }
		saved
			.andExpect(status().isOk)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.globalId").value(storedId))
			.andExpect(jsonPath("$.origin").value("Alerts"))
			.andExpect(jsonPath("$.rank").value(0.5))
			.andExpect(jsonPath("$.position").value(4))
			.andExpect(jsonPath("$.ownerGlobalId").value(nullValue()))
			.andExpect(jsonPath("$.id").isNumber())
		runBlocking { assertEquals(1L, countOrigins()) }
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `post updates an origin matched by a title`() {
		// Arrange
		val origin = "PayamaX"
		val owner = UUID.randomUUID()
		val globalId = runBlocking {
			insert(origin, position = 1, rank = 1.0, owner = null)
			storedGlobalId(origin)
		}
		val body = originJson(
			origin = origin,
			rank = 0.25,
			position = 6,
			globalId = UUID.fromString(globalId),
			ownerGlobalId = owner,
		)

		// Act
		val saved = submit(post(ORIGINS), body)

		// Assert
		saved
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.globalId").value(globalId))
			.andExpect(jsonPath("$.origin").value(origin))
			.andExpect(jsonPath("$.rank").value(0.25))
			.andExpect(jsonPath("$.position").value(6))
			.andExpect(jsonPath("$.ownerGlobalId").value(owner.toString()))
			.andExpect(jsonPath("$.id").isNumber())
		runBlocking {
			assertEquals(1L, countOrigins())
			assertEquals(globalId, storedGlobalId(origin))
			assertEquals(0.25, storedRank(origin))
		}
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `post updates an origin matched by a gsm number`() {
		// Arrange
		val origin = "09121234567"
		val owner = UUID.randomUUID()
		val globalId = runBlocking {
			insert(origin, position = 1, rank = 1.0, owner = null)
			storedGlobalId(origin)
		}
		val body = originJson(
			origin = origin,
			rank = 0.75,
			position = 7,
			globalId = UUID.fromString(globalId),
			ownerGlobalId = owner,
		)

		// Act
		val saved = submit(post(ORIGINS), body)

		// Assert
		saved
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.globalId").value(globalId))
			.andExpect(jsonPath("$.origin").value(origin))
			.andExpect(jsonPath("$.rank").value(0.75))
			.andExpect(jsonPath("$.position").value(7))
			.andExpect(jsonPath("$.ownerGlobalId").value(owner.toString()))
			.andExpect(jsonPath("$.id").isNumber())
		runBlocking {
			assertEquals(1L, countOrigins())
			assertEquals(globalId, storedGlobalId(origin))
			assertEquals(0.75, storedRank(origin))
		}
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `post rejects a global id that does not match the stored origin`() {
		// Arrange
		val origin = "PayamaX"
		val storedId = runBlocking {
			insert(origin, position = 1, rank = 0.5, owner = null)
			storedGlobalId(origin)
		}
		val body = originJson(origin = origin, rank = 0.75, position = 1, globalId = UUID.randomUUID())

		// Act
		val saved = submit(post(ORIGINS), body)

		// Assert
		saved
			.andExpect(status().isBadRequest)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.title").value("Bad Request"))
			.andExpect(jsonPath("$.detail").value("Invalid value"))
		runBlocking {
			assertEquals(1L, countOrigins())
			assertEquals(storedId, storedGlobalId(origin))
			assertEquals(0.5, storedRank(origin))
		}
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `post accepts rank 0 and rank 1`() {
		// Arrange
		val lowest = originJson(origin = "Alerts", rank = 0.0, position = 1)
		val highest = originJson(origin = "Notices", rank = 1.0, position = 2)

		// Act
		val savedLowest = submit(post(ORIGINS), lowest)
		val savedHighest = submit(post(ORIGINS), highest)

		// Assert
		savedLowest
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.origin").value("Alerts"))
			.andExpect(jsonPath("$.rank").value(0.0))
		savedHighest
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.origin").value("Notices"))
			.andExpect(jsonPath("$.rank").value(1.0))
		runBlocking {
			assertEquals(0.0, storedRank("Alerts"))
			assertEquals(1.0, storedRank("Notices"))
		}
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `post rejects a rank outside 0 to 1`() {
		// Arrange
		val origin = "PayamaX"
		runBlocking { insert(origin, position = 1, rank = 0.5, owner = null) }
		val below = originJson(origin = "Alerts", rank = -0.01, position = 2)
		val above = originJson(origin = origin, rank = 1.01, position = 1)

		// Act
		val rejectedBelow = submit(post(ORIGINS), below)
		val rejectedAbove = submit(post(ORIGINS), above)

		// Assert
		rejectedBelow
			.andExpect(status().isBadRequest)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.title").value("Bad Request"))
			.andExpect(jsonPath("$.detail").value("Rank must be between 0 and 1."))
		rejectedAbove
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.detail").value("Rank must be between 0 and 1."))
		runBlocking {
			assertEquals(0L, countOrigins("Alerts"))
			assertEquals(0.5, storedRank(origin))
		}
	}

	@Test
	@WithMockUser(roles = ["USER"])
	fun `post rejects a caller without ROLE_ADMIN`() {
		// Arrange
		val body = originJson(origin = "Alerts", rank = 0.5, position = 4)

		// Act
		val saved = submit(post(ORIGINS), body)

		// Assert
		saved.expectForbidden()
		runBlocking { assertEquals(0L, countOrigins()) }
	}

	@Test
	fun `post rejects a caller with no credentials`() {
		// Arrange
		val body = originJson(origin = "Alerts", rank = 0.5, position = 4)

		// Act
		val saved = submit(post(ORIGINS), body)

		// Assert
		saved.expectForbidden()
		runBlocking { assertEquals(0L, countOrigins()) }
	}

	@Test
	fun `post rejects an untrusted audience`() {
		// Arrange
		val token = untrustedToken()
		val body = originJson(origin = "Alerts", rank = 0.5, position = 4)

		// Act
		val saved = submit(post(ORIGINS), body, token = token)

		// Assert
		saved.andExpect(status().isUnauthorized)
		runBlocking { assertEquals(0L, countOrigins()) }
	}

	@Test
	fun `post rejects a malformed token`() {
		// Arrange
		val body = originJson(origin = "Alerts", rank = 0.5, position = 4)

		// Act
		val saved = submit(post(ORIGINS), body, token = "not-a-jwt")

		// Assert
		saved.andExpect(status().isUnauthorized)
		runBlocking { assertEquals(0L, countOrigins()) }
	}
}
