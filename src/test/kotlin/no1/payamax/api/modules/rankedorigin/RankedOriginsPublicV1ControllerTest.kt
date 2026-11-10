package no1.payamax.api.modules.rankedorigin

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import no1.payamax.api.TestsSuper
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class RankedOriginsPublicV1ControllerTest : TestsSuper() {
	@field:Autowired
	private lateinit var mockMvc: MockMvc

	@field:Autowired
	private lateinit var databaseClient: DatabaseClient

	@BeforeEach
	fun clearOrigins() {
		runBlocking {
			databaseClient.sql("DELETE FROM ranked_origin").fetch().rowsUpdated().awaitSingle()
		}
	}

	@Test
	fun `anonymous callers receive only public origins`() {
		// Arrange
		val publicIds = runBlocking {
			insert("alpha", position = 1, rank = 1.5, owner = null)
			insert("beta", position = 2, rank = 2.5, owner = null)
			insert("owned-origin", position = 9, rank = 9.0, owner = UUID.randomUUID())
			listOf(storedGlobalId("alpha"), storedGlobalId("beta"))
		}

		// Act
		val page = getPage()

		// Assert
		page
			.andExpect(status().isOk)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.number").value(0))
			.andExpect(jsonPath("$.content.length()").value(2))
			.andExpect(jsonPath("$.content[*].globalId", containsInAnyOrder(publicIds[0], publicIds[1])))
			.andExpect(jsonPath("$.content[*].origin", containsInAnyOrder("alpha", "beta")))
			.andExpect(jsonPath("$.content[*].origin", not(hasItem("owned-origin"))))
			.andExpect(jsonPath("$.content[0].ownerGlobalId").doesNotExist())
			.andExpect(jsonPath("$.content[0].id").doesNotExist())
	}

	@Test
	fun `a trusted token can read the same public page`() {
		// Arrange
		runBlocking {
			insert("alpha", position = 1, rank = 1.5, owner = null)
			insert("owned-origin", position = 9, rank = 9.0, owner = UUID.randomUUID())
		}
		val token = readerToken()

		// Act
		val page = getPage(token = token)

		// Assert
		page
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].origin").value("alpha"))
			.andExpect(jsonPath("$.content[0].position").value(1))
			.andExpect(jsonPath("$.content[0].rank").value(1.5))
			.andExpect(jsonPath("$.content[*].origin", not(hasItem("owned-origin"))))
	}

	@Test
	fun `a token for an untrusted audience is rejected`() {
		// Arrange
		val token = untrustedToken()

		// Act
		val page = getPage(token = token)

		// Assert
		page.andExpect(status().isUnauthorized)
	}

	@Test
	fun `a malformed token is rejected`() {
		// Arrange
		val token = "not-a-jwt"

		// Act
		val page = getPage(token = token)

		// Assert
		page.andExpect(status().isUnauthorized)
	}

	@Test
	fun `public origins are paged and sorted by position`() {
		// Arrange
		runBlocking {
			insert("gamma", position = 3, rank = 3.0, owner = null)
			insert("alpha", position = 1, rank = 1.0, owner = null)
			insert("beta", position = 2, rank = 2.0, owner = null)
			insert("owned-origin", position = 4, rank = 4.0, owner = UUID.randomUUID())
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
			.andExpect(jsonPath("$.content[0].origin").value("alpha"))
			.andExpect(jsonPath("$.content[0].position").value(1))
			.andExpect(jsonPath("$.content[0].rank").value(1.0))
			.andExpect(jsonPath("$.content[1].origin").value("beta"))
			.andExpect(jsonPath("$.content[1].position").value(2))

		// Act
		val secondPage = getPage(page = 1, size = 2, sort = "position,asc")

		// Assert
		secondPage
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.number").value(1))
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].origin").value("gamma"))
			.andExpect(jsonPath("$.content[0].position").value(3))

		// Act
		val pastTheEnd = getPage(page = 5, size = 2, sort = "position,asc")

		// Assert
		pastTheEnd
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.content.length()").value(0))
	}

	@Test
	fun `an unknown sort property is a bad request`() {
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
	fun `an empty catalog is an empty page`() {
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

	private fun getPage(token: String? = null, page: Int = 0, size: Int = 20, sort: String? = null): ResultActions {
		var request = get("/v1/public/ranked-origins")
			.param("page", page.toString())
			.param("size", size.toString())
			.accept(MediaType.APPLICATION_JSON)
		if (sort != null) {
			request = request.param("sort", sort)
		}
		if (token != null) {
			request = request.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
		}
		val actions = mockMvc.perform(request)
		val result = actions.andReturn()
		return if (result.request.isAsyncStarted) mockMvc.perform(asyncDispatch(result)) else actions
	}

	private suspend fun insert(origin: String, position: Int, rank: Double, owner: UUID?) {
		var spec = databaseClient.sql(
			"""
			INSERT INTO ranked_origin (global_id, origin, owner_global_id, position, rank)
			VALUES (:globalId, :origin, :owner, :position, :rank)
			""".trimIndent(),
		)
			.bind("globalId", UUID.randomUUID())
			.bind("origin", origin)
			.bind("position", position)
			.bind("rank", rank)
		spec = if (owner == null) spec.bindNull("owner", UUID::class.java) else spec.bind("owner", owner)
		spec.fetch().rowsUpdated().awaitSingle()
	}

	private suspend fun storedGlobalId(origin: String): String {
		return databaseClient.sql("SELECT global_id FROM ranked_origin WHERE origin = :origin")
			.bind("origin", origin)
			.map { row, _ -> row.get("global_id", UUID::class.java)!!.toString() }
			.one()
			.awaitSingle()
	}

}
