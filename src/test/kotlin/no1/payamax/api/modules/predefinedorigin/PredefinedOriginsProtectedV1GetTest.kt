package no1.payamax.api.modules.predefinedorigin

import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class PredefinedOriginsProtectedV1GetTest : PredefinedOriginsProtectedV1TestBase() {
	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `get returns every origin, including owned ones`() {
		// Arrange
		val owner = UUID.randomUUID()
		val ids = runBlocking {
			insert("PayamaX", position = 1, rank = 0.25, owner = null)
			insert("09121234567", position = 2, rank = 0.75, owner = owner)
			listOf(storedGlobalId("PayamaX"), storedGlobalId("09121234567"))
		}

		// Act
		val page = getPage(sort = "position,asc")

		// Assert
		page
			.andExpect(status().isOk)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.number").value(0))
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.content[0].globalId").value(ids[0]))
			.andExpect(jsonPath("$.content[0].origin").value("PayamaX"))
			.andExpect(jsonPath("$.content[0].rank").value(0.25))
			.andExpect(jsonPath("$.content[0].position").value(1))
			.andExpect(jsonPath("$.content[0].ownerGlobalId").value(nullValue()))
			.andExpect(jsonPath("$.content[0].id").isNumber())
			.andExpect(jsonPath("$.content[1].globalId").value(ids[1]))
			.andExpect(jsonPath("$.content[1].origin").value("09121234567"))
			.andExpect(jsonPath("$.content[1].rank").value(0.75))
			.andExpect(jsonPath("$.content[1].position").value(2))
			.andExpect(jsonPath("$.content[1].ownerGlobalId").value(owner.toString()))
			.andExpect(jsonPath("$.content[1].id").isNumber())
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `get pages and sorts by position`() {
		// Arrange
		runBlocking {
			insert("09121234567", position = 3, rank = 0.75, owner = UUID.randomUUID())
			insert("PayamaX", position = 1, rank = 0.25, owner = null)
			insert("Alerts", position = 2, rank = 0.5, owner = null)
		}

		// Act
		val firstPage = getPage(page = 0, size = 2, sort = "position,asc")

		// Assert
		firstPage
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.totalPages").value(2))
			.andExpect(jsonPath("$.size").value(2))
			.andExpect(jsonPath("$.number").value(0))
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.content[0].origin").value("PayamaX"))
			.andExpect(jsonPath("$.content[0].position").value(1))
			.andExpect(jsonPath("$.content[1].origin").value("Alerts"))
			.andExpect(jsonPath("$.content[1].position").value(2))

		// Act
		val secondPage = getPage(page = 1, size = 2, sort = "position,asc")

		// Assert
		secondPage
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.number").value(1))
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].origin").value("09121234567"))
			.andExpect(jsonPath("$.content[0].ownerGlobalId").exists())

		// Act
		val pastTheEnd = getPage(page = 5, size = 2, sort = "position,asc")

		// Assert
		pastTheEnd
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.content.length()").value(0))
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `get rejects an unknown sort property`() {
		// Arrange
		val sort = """["string"]"""

		// Act
		val page = getPage(sort = sort)

		// Assert
		page
			.andExpect(status().isBadRequest)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.title").value("Bad Request"))
			.andExpect(jsonPath("$.detail").value("""Invalid sort property '["string"]'."""))
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `get returns an empty page when nothing is stored`() {
		// Arrange
		// clearOrigins() leaves the catalog empty.

		// Act
		val page = getPage()

		// Assert
		page
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.totalElements").value(0))
			.andExpect(jsonPath("$.content.length()").value(0))
	}

	@Test
	@WithMockUser(roles = ["USER"])
	fun `get rejects a caller without ROLE_ADMIN`() {
		// Arrange
		// The caller is authenticated, without the admin role.

		// Act
		val page = getPage()

		// Assert
		page.expectForbidden()
	}

	@Test
	fun `get rejects a caller with no credentials`() {
		// Arrange
		// No token and no authenticated test user.

		// Act
		val page = getPage()

		// Assert
		page.expectForbidden()
	}

	@Test
	fun `get rejects an untrusted audience`() {
		// Arrange
		val token = untrustedToken()

		// Act
		val page = getPage(token = token)

		// Assert
		page.andExpect(status().isUnauthorized)
	}

	@Test
	fun `get rejects a malformed token`() {
		// Arrange
		val token = "not-a-jwt"

		// Act
		val page = getPage(token = token)

		// Assert
		page.andExpect(status().isUnauthorized)
	}
}
