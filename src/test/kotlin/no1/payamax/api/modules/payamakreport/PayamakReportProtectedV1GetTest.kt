package no1.payamax.api.modules.payamakreport

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import no1.payamax.api.TestsSuper
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class PayamakReportProtectedV1GetTest : TestsSuper() {
	@field:Autowired
	private lateinit var mockMvc: MockMvc

	@field:Autowired
	private lateinit var databaseClient: DatabaseClient

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `get returns every report for ROLE_ADMIN`() {
		// Arrange
		val reports = runBlocking { insertSamples() }

		// Act
		val page = getReports()

		// Assert
		page.expectOkPage(total = reports.size)
		reports.forEachIndexed { index, report -> page.expectReport(index, report) }
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `get returns an empty page when nothing is stored`() {
		// Arrange
		// clearDatabase() leaves the catalog empty.

		// Act
		val page = getReports()

		// Assert
		page.expectOkPage(total = 0)
	}

	@Test
	@WithMockUser(roles = ["USER"])
	fun `get rejects a caller without ROLE_ADMIN`() {
		// Arrange
		// The caller is authenticated, without the admin role.

		// Act
		val page = getReports()

		// Assert
		page.expectForbidden()
	}

	@Test
	fun `get rejects a caller with no credentials`() {
		// Arrange
		// No token and no authenticated test user.

		// Act
		val page = getReports()

		// Assert
		page.expectForbidden()
	}

	@Test
	fun `get rejects an untrusted audience`() {
		// Arrange
		val token = untrustedToken()

		// Act
		val page = getReports(token = token)

		// Assert
		page.andExpect(status().isUnauthorized)
	}

	@Test
	fun `get rejects a malformed token`() {
		// Arrange
		val token = "not-a-jwt"

		// Act
		val page = getReports(token = token)

		// Assert
		page.andExpect(status().isUnauthorized)
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `get filters by state`() {
		// Arrange
		runBlocking { insertSamples() }

		// Act
		val page = getReports(state = listOf("PENDING"))

		// Assert
		page.expectOkPage(total = 2)
			.expectReport(0, origin = "Alerts", state = "PENDING", flagged = false)
			.expectReport(1, origin = "PayamaX", state = "PENDING", flagged = true)
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `get filters by more than one state`() {
		// Arrange
		runBlocking { insertSamples() }

		// Act
		val page = getReports(state = listOf("PENDING", "POSTPONED"))

		// Assert
		page.expectOkPage(total = 3)
			.expectReport(0, origin = "Alerts", state = "PENDING", flagged = false)
			.expectReport(1, origin = "PayamaX", state = "PENDING", flagged = true)
			.expectReport(2, origin = "Queue", state = "POSTPONED", flagged = true)
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `get filters by origin`() {
		// Arrange
		runBlocking { insertSamples() }

		// Act
		val page = getReports(origin = "PayamaX")

		// Assert
		page.expectOkPage(total = 1)
			.expectReport(0, origin = "PayamaX", state = "PENDING", flagged = true)
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `get filters by global id`() {
		// Arrange
		val reports = runBlocking { insertSamples() }
		val notices = reports.first { it.origin == "Notices" }

		// Act
		val page = getReports(globalId = notices.globalId)

		// Assert
		page.expectOkPage(total = 1).expectReport(0, notices)
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `get filters to flagged reports`() {
		// Arrange
		runBlocking { insertSamples() }

		// Act
		val page = getReports(onlyFlagged = true)

		// Assert
		page.expectOkPage(total = 2)
			.expectReport(0, origin = "PayamaX", state = "PENDING", flagged = true)
			.expectReport(1, origin = "Queue", state = "POSTPONED", flagged = true)
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `get keeps flagged reports when flagged is false`() {
		// Arrange
		val reports = runBlocking { insertSamples() }

		// Act
		val page = getReports(onlyFlagged = false)

		// Assert
		page.expectOkPage(total = reports.size)
		reports.forEachIndexed { index, report -> page.expectReport(index, report) }
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `get applies state, origin, and flagged together`() {
		// Arrange
		runBlocking { insertSamples() }

		// Act
		val page = getReports(state = listOf("PENDING"), origin = "PayamaX", onlyFlagged = true)

		// Assert
		page.expectOkPage(total = 1)
			.expectReport(0, origin = "PayamaX", state = "PENDING", flagged = true)
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `get returns an empty page when nothing matches`() {
		// Arrange
		runBlocking { insertSamples() }

		// Act
		val page = getReports(origin = "Missing", globalId = UUID.randomUUID(), state = listOf("REVIEWED"))

		// Assert
		page.expectOkPage(total = 0)
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `get rejects an unknown state`() {
		// Arrange
		val state = "NOPE"

		// Act
		val page = getReports(state = listOf(state))

		// Assert
		page
			.andExpect(status().isBadRequest)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.title").value("Bad Request"))
			.andExpect(jsonPath("$.detail").value("Invalid request content."))
			.andExpect(jsonPath("$.errors[0].field").value("state"))
			.andExpect(jsonPath("$.errors[0].message", containsString(state)))
	}

	private fun getReports(
		token: String? = null,
		state: List<String>? = null,
		globalId: UUID? = null,
		origin: String? = null,
		onlyFlagged: Boolean? = null,
	): ResultActions {
		var request = get(REPORTS)
			.param("page", "0")
			.param("size", "20")
			.param("sort", "origin,asc")
			.accept(MediaType.APPLICATION_JSON)
		state?.forEach { request = request.param("state", it) }
		if (globalId != null) {
			request = request.param("globalId", globalId.toString())
		}
		if (origin != null) {
			request = request.param("origin", origin)
		}
		if (onlyFlagged != null) {
			request = request.param("onlyFlagged", onlyFlagged.toString())
		}
		if (token != null) {
			request = request.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
		}
		return exchange(request)
	}

	private fun exchange(request: RequestBuilder): ResultActions {
		val actions = mockMvc.perform(request)
		val result = actions.andReturn()
		return if (result.request.isAsyncStarted) mockMvc.perform(asyncDispatch(result)) else actions
	}

	private fun ResultActions.expectForbidden(): ResultActions {
		return andExpect(status().isForbidden)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(403))
			.andExpect(jsonPath("$.title").value("Forbidden"))
			.andExpect(jsonPath("$.detail").value("Access denied"))
	}

	private fun ResultActions.expectOkPage(total: Int): ResultActions {
		return andExpect(status().isOk)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.page.totalElements").value(total))
			.andExpect(jsonPath("$.page.number").value(0))
			.andExpect(jsonPath("$.content.length()").value(total))
	}

	private fun ResultActions.expectReport(index: Int, report: StoredReport): ResultActions =
		expectReport(index, report.origin, report.state, report.flagged, report.globalId)

	private fun ResultActions.expectReport(
		index: Int,
		origin: String,
		state: String,
		flagged: Boolean,
		globalId: UUID? = null,
	): ResultActions {
		var checks = andExpect(jsonPath("$.content[$index].origin").value(origin))
			.andExpect(jsonPath("$.content[$index].state").value(state))
			.andExpect(jsonPath("$.content[$index].flagged").value(flagged))
			.andExpect(jsonPath("$.content[$index].detectedDiagnostics").value("Spam"))
		if (globalId != null) {
			checks = checks.andExpect(jsonPath("$.content[$index].globalId").value(globalId.toString()))
		}
		return checks
	}

	private suspend fun insertSamples(): List<StoredReport> = listOf(
		insert(origin = "Alerts", state = "PENDING", flagged = false),
		insert(origin = "Notices", state = "REVIEWED", flagged = false),
		insert(origin = "PayamaX", state = "PENDING", flagged = true),
		insert(origin = "Queue", state = "POSTPONED", flagged = true),
	)

	private suspend fun insert(origin: String, state: String, flagged: Boolean): StoredReport {
		val globalId = UUID.randomUUID()
		databaseClient.sql(
			"""
			INSERT INTO payamak_report (global_id, origin, state, flagged, detected_diagnostics, body)
			VALUES (:globalId, :origin, :state, :flagged, :detected, :body)
			""".trimIndent(),
		)
			.bind("globalId", globalId)
			.bind("origin", origin)
			.bind("state", state)
			.bind("flagged", flagged)
			.bind("detected", "Spam")
			.bind("body", "hello from $origin")
			.fetch()
			.rowsUpdated()
			.awaitSingle()
		return StoredReport(globalId, origin, state, flagged)
	}

	private data class StoredReport(
		val globalId: UUID,
		val origin: String,
		val state: String,
		val flagged: Boolean,
	)

	private companion object {
		const val REPORTS = "/v1/protected/payamak-reports"
	}
}
