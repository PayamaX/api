package no1.payamax.api.modules.rankedorigin

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import no1.payamax.api.TestsSuper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
abstract class RankedOriginsProtectedV1TestBase : TestsSuper() {
	@field:Autowired
	protected lateinit var mockMvc: MockMvc

	@field:Autowired
	protected lateinit var databaseClient: DatabaseClient

	protected fun getOrigin(globalId: UUID, token: String? = null): ResultActions {
		var request = get("$ORIGINS/$globalId").accept(MediaType.APPLICATION_JSON)
		if (token != null) {
			request = request.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
		}
		return exchange(request)
	}

	protected fun getPage(token: String? = null, page: Int = 0, size: Int = 20, sort: String? = null): ResultActions {
		var request = get(ORIGINS)
			.param("page", page.toString())
			.param("size", size.toString())
			.accept(MediaType.APPLICATION_JSON)
		if (sort != null) {
			request = request.param("sort", sort)
		}
		if (token != null) {
			request = request.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
		}
		return exchange(request)
	}

	protected fun submit(builder: MockHttpServletRequestBuilder, body: String, token: String? = null): ResultActions {
		var request = builder
			.contentType(MediaType.APPLICATION_JSON)
			.accept(MediaType.APPLICATION_JSON)
			.content(body)
		if (token != null) {
			request = request.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
		}
		return exchange(request)
	}

	protected fun deleteOrigin(globalId: UUID, token: String? = null): ResultActions {
		var request = delete("$ORIGINS/$globalId").accept(MediaType.APPLICATION_JSON)
		if (token != null) {
			request = request.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
		}
		return exchange(request)
	}

	protected fun deleteOrigins(globalIds: List<UUID>, token: String? = null): ResultActions {
		var request = delete(ORIGINS).accept(MediaType.APPLICATION_JSON)
		if (globalIds.isNotEmpty()) {
			request = request.param("globalIds", *globalIds.map { it.toString() }.toTypedArray())
		}
		if (token != null) {
			request = request.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
		}
		return exchange(request)
	}

	protected fun exchange(request: RequestBuilder): ResultActions {
		val actions = mockMvc.perform(request)
		val result = actions.andReturn()
		return if (result.request.isAsyncStarted) mockMvc.perform(asyncDispatch(result)) else actions
	}

	protected fun ResultActions.expectForbidden(): ResultActions {
		return andExpect(status().isForbidden)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(403))
			.andExpect(jsonPath("$.title").value("Forbidden"))
			.andExpect(jsonPath("$.detail").value("Access denied"))
	}

	protected fun originJson(
		origin: String,
		rank: Double,
		position: Int,
		globalId: UUID? = null,
		ownerGlobalId: UUID? = null,
	): String {
		val globalIdJson = globalId?.let { "\"$it\"" } ?: "null"
		val ownerJson = ownerGlobalId?.let { "\"$it\"" } ?: "null"
		return """{"globalId":$globalIdJson,"origin":"$origin","rank":$rank,"ownerGlobalId":$ownerJson,"position":$position}"""
	}

	protected suspend fun insert(origin: String, position: Int, rank: Double, owner: UUID?) {
		var spec = databaseClient.sql(
			"""
			INSERT INTO ranked_origin (global_id, origin, owner_global_id, position, rank, updated)
			VALUES (:globalId, :origin, :owner, :position, :rank, :updated)
			""".trimIndent(),
		)
			.bind("globalId", UUID.randomUUID())
			.bind("origin", origin)
			.bind("position", position)
			.bind("rank", rank)
			.bind("updated", LocalDateTime.of(2020, 1, 1, 0, 0))
		spec = if (owner == null) spec.bindNull("owner", UUID::class.java) else spec.bind("owner", owner)
		spec.fetch().rowsUpdated().awaitSingle()
	}

	protected suspend fun storedGlobalId(origin: String): String {
		return databaseClient.sql("SELECT global_id FROM ranked_origin WHERE origin = :origin")
			.bind("origin", origin)
			.map { row, _ -> row.get("global_id", UUID::class.java)!!.toString() }
			.one()
			.awaitSingle()
	}

	protected suspend fun storedRank(origin: String): Double {
		return databaseClient.sql("SELECT rank FROM ranked_origin WHERE origin = :origin")
			.bind("origin", origin)
			.map { row, _ -> (row.get("rank") as Number).toDouble() }
			.one()
			.awaitSingle()
	}

	protected suspend fun isDeleted(origin: String): Boolean {
		return databaseClient.sql("SELECT deleted FROM ranked_origin WHERE origin = :origin")
			.bind("origin", origin)
			.map { row, _ -> row.get("deleted") != null }
			.one()
			.awaitSingle()
	}

	protected suspend fun countOrigins(origin: String? = null): Long {
		val sql = if (origin == null) {
			"SELECT COUNT(*) AS count FROM ranked_origin"
		} else {
			"SELECT COUNT(*) AS count FROM ranked_origin WHERE origin = :origin"
		}
		var spec = databaseClient.sql(sql)
		if (origin != null) {
			spec = spec.bind("origin", origin)
		}
		return spec.map { row, _ -> (row.get("count") as Number).toLong() }.one().awaitSingle()
	}

	protected companion object {
		const val ORIGINS = "/v1/protected/ranked-origins"
	}
}
