package no1.payamax.api.modules.payamakreport

import kotlinx.coroutines.reactor.awaitSingle
import no1.payamax.api.TestsSuper
import org.hamcrest.Matchers.nullValue
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
abstract class PayamakReportProtectedV1TestBase : TestsSuper() {
	@field:Autowired
	protected lateinit var mockMvc: MockMvc

	@field:Autowired
	protected lateinit var databaseClient: DatabaseClient

	protected fun exchange(request: RequestBuilder): ResultActions {
		val actions = mockMvc.perform(request)
		val result = actions.andReturn()
		return if (result.request.isAsyncStarted) mockMvc.perform(asyncDispatch(result)) else actions
	}

	protected fun withToken(request: MockHttpServletRequestBuilder, token: String?): MockHttpServletRequestBuilder {
		if (token == null) {
			return request
		}
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
	}

	protected fun ResultActions.expectReport(
		globalId: UUID,
		state: String,
		flagged: Boolean,
		reviewerComment: String? = null,
		decidedDiagnostics: String? = null,
	): ResultActions {
		var checks = andExpect(status().isOk)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.globalId").value(globalId.toString()))
			.andExpect(jsonPath("$.state").value(state))
			.andExpect(jsonPath("$.flagged").value(flagged))
			.andExpect(jsonPath("$.detectedDiagnostics").value("Spam"))
		checks = if (reviewerComment == null) {
			checks.andExpect(jsonPath("$.reviewerComment").value(nullValue()))
		} else {
			checks.andExpect(jsonPath("$.reviewerComment").value(reviewerComment))
		}
		checks = if (decidedDiagnostics == null) {
			checks.andExpect(jsonPath("$.decidedDiagnostics").value(nullValue()))
		} else {
			checks.andExpect(jsonPath("$.decidedDiagnostics").value(decidedDiagnostics))
		}
		return checks
	}

	protected fun ResultActions.expectMissing(instance: String): ResultActions {
		return andExpect(status().isBadRequest)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.title").value("Bad Request"))
			.andExpect(jsonPath("$.detail").value("Entity not found"))
			.andExpect(jsonPath("$.instance").value(instance))
	}

	protected fun ResultActions.expectForbidden(): ResultActions {
		return andExpect(status().isForbidden)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(403))
			.andExpect(jsonPath("$.title").value("Forbidden"))
			.andExpect(jsonPath("$.detail").value("Access denied"))
	}

	protected suspend fun insert(
		origin: String = "PayamaX",
		state: String = "PENDING",
		flagged: Boolean = false,
		reviewerComment: String? = null,
		decidedDiagnostics: String? = null,
	): UUID {
		val globalId = UUID.randomUUID()
		var spec = databaseClient.sql(
			"""
			INSERT INTO payamak_report (
				global_id, origin, state, flagged, detected_diagnostics, body, reviewer_comment, decided_diagnostics
			) VALUES (
				:globalId, :origin, :state, :flagged, :detected, :body, :reviewerComment, :decidedDiagnostics
			)
			""".trimIndent(),
		)
			.bind("globalId", globalId)
			.bind("origin", origin)
			.bind("state", state)
			.bind("flagged", flagged)
			.bind("detected", "Spam")
			.bind("body", "hello from $origin")
		spec = if (reviewerComment == null) spec.bindNull("reviewerComment", String::class.java) else spec.bind("reviewerComment", reviewerComment)
		spec = if (decidedDiagnostics == null) spec.bindNull("decidedDiagnostics", String::class.java) else spec.bind("decidedDiagnostics", decidedDiagnostics)
		spec.fetch().rowsUpdated().awaitSingle()
		return globalId
	}

	protected suspend fun stored(globalId: UUID): StoredReport {
		return databaseClient.sql(
			"""
			SELECT origin, state, flagged, reviewer_comment, decided_diagnostics
			FROM payamak_report
			WHERE global_id = :globalId
			""".trimIndent(),
		)
			.bind("globalId", globalId)
			.map { row, _ ->
				StoredReport(
					origin = row.get("origin", String::class.java)!!,
					state = row.get("state", String::class.java)!!,
					flagged = row.get("flagged", Boolean::class.java)!!,
					reviewerComment = row.get("reviewer_comment", String::class.java),
					decidedDiagnostics = row.get("decided_diagnostics", String::class.java),
				)
			}
			.one()
			.awaitSingle()
	}

	protected data class StoredReport(
		val origin: String,
		val state: String,
		val flagged: Boolean,
		val reviewerComment: String?,
		val decidedDiagnostics: String?,
	)

	protected companion object {
		const val REPORTS = "/v1/protected/payamak-reports"
	}
}
