package no1.payamax.api.modules.payamakreport

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import no1.payamax.api.TestsSuper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@AutoConfigureMockMvc
class PayamakReportPublicV1PostTest : TestsSuper() {
	@field:Autowired
	private lateinit var mockMvc: MockMvc

	@field:Autowired
	private lateinit var databaseClient: DatabaseClient

	@Test
	fun `post rejects an empty item list`() {
		// Arrange
		val body = """{"items":[]}"""

		// Act
		val response = submit(body)

		// Assert
		expectItemCountRejected(response)
		runBlocking { assertNothingStored() }
	}

	@Test
	fun `post rejects more than 100 items`() {
		// Arrange
		val body = reportsJson(count = 101)

		// Act
		val response = submit(body)

		// Assert
		expectItemCountRejected(response)
		runBlocking { assertNothingStored() }
	}

	@Test
	fun `post stores a report without credentials`() {
		// Arrange
		val reporter = UUID.fromString("11111111-1111-1111-1111-111111111111")
		val body = reportJson(origin = "Anonymous", reporter = reporter)

		// Act
		val response = submit(body)

		// Assert
		response.andExpect(status().isOk).andExpect(content().string(""))
		runBlocking { assertStored(origin = "Anonymous", reporter = reporter) }
	}

	@Test
	fun `post stores a report for an authenticated caller`() {
		// Arrange
		val reporter = UUID.fromString("22222222-2222-2222-2222-222222222222")
		val body = reportJson(origin = "Signed in", reporter = reporter)

		// Act
		val response = submit(body, token = readerToken())

		// Assert
		response.andExpect(status().isOk).andExpect(content().string(""))
		runBlocking { assertStored(origin = "Signed in", reporter = reporter) }
	}

	private fun submit(body: String, token: String? = null): ResultActions {
		var request = post(REPORTS)
			.contentType(MediaType.APPLICATION_JSON)
			.accept(MediaType.APPLICATION_JSON)
			.content(body)
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

	private fun expectItemCountRejected(response: ResultActions) {
		response
			.andExpect(status().isBadRequest)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.title").value("Bad Request"))
			.andExpect(jsonPath("$.detail").value("Invalid request content."))
			.andExpect(jsonPath("$.errors.length()").value(1))
			.andExpect(jsonPath("$.errors[0].field").value("items"))
			.andExpect(jsonPath("$.errors[0].message").value("size must be between 1 and 100"))
	}

	private suspend fun assertNothingStored() {
		assertEquals(0L, count("payamak_report"))
		assertEquals(0L, count("payamak_rule_process_result_report"))
	}

	private suspend fun assertStored(origin: String, reporter: UUID) {
		assertEquals(1L, count("payamak_report"))
		val report = storedReport(origin)
		assertEquals(origin, report.origin)
		assertEquals("hello from $origin", report.body)
		assertEquals("abc123", report.sha512)
		assertEquals(reporter, report.reporterUserGlobalId)
		assertEquals("seen on device", report.reporterComment)
		assertEquals("Spam", report.detectedDiagnostics)
		assertEquals("Useful", report.expectedDiagnostics)
		assertNotNull(report.globalId)

		val results = storedResults(report.id)
		assertEquals(
			listOf(
				StoredResult(report.id, "rule-a", "1.2", 0.25f, 0.5f, "kept", 1),
				StoredResult(report.id, "rule-b", "3", 1.0f, null, "auto", 2),
			),
			results,
		)
	}

	private suspend fun storedReport(origin: String): StoredReport {
		return databaseClient.sql(
			"""
			SELECT id, global_id, origin, body, sha512, reporter_user_global_id, reporter_comment,
			       detected_diagnostics, expected_diagnostics
			FROM payamak_report
			WHERE origin = :origin
			""".trimIndent(),
		)
			.bind("origin", origin)
			.map { row, _ ->
				StoredReport(
					id = (row.get("id") as Number).toLong(),
					globalId = row.get("global_id", UUID::class.java),
					origin = row.get("origin", String::class.java)!!,
					body = row.get("body", String::class.java),
					sha512 = row.get("sha512", String::class.java),
					reporterUserGlobalId = row.get("reporter_user_global_id", UUID::class.java),
					reporterComment = row.get("reporter_comment", String::class.java),
					detectedDiagnostics = row.get("detected_diagnostics", String::class.java)!!,
					expectedDiagnostics = row.get("expected_diagnostics", String::class.java),
				)
			}
			.one()
			.awaitSingle()
	}

	private suspend fun storedResults(reportId: Long): List<StoredResult> {
		return databaseClient.sql(
			"""
			SELECT report_id, processor_name, processor_version, general_rank, user_override_rank,
			       processor_provided_comment, position
			FROM payamak_rule_process_result_report
			WHERE report_id = :reportId
			ORDER BY position
			""".trimIndent(),
		)
			.bind("reportId", reportId)
			.map { row, _ ->
				StoredResult(
					reportId = (row.get("report_id") as Number).toLong(),
					processorName = row.get("processor_name", String::class.java)!!,
					processorVersion = row.get("processor_version", String::class.java)!!,
					generalRank = (row.get("general_rank") as Number).toFloat(),
					userOverrideRank = (row.get("user_override_rank") as Number?)?.toFloat(),
					processorProvidedComment = row.get("processor_provided_comment", String::class.java)!!,
					position = (row.get("position") as Number).toInt(),
				)
			}
			.all()
			.collectList()
			.awaitSingle()
	}

	private suspend fun count(table: String): Long {
		return databaseClient.sql("SELECT COUNT(*) AS count FROM $table")
			.map { row, _ -> (row.get("count") as Number).toLong() }
			.one()
			.awaitSingle()
	}

	private fun reportJson(origin: String, reporter: UUID): String = """{"items":[${itemJson(origin, reporter)}]}"""

	private fun reportsJson(count: Int): String {
		val item = itemJson(origin = "bulk", reporter = UUID.fromString("00000000-0000-0000-0000-000000000001"))
		return """{"items":[${List(count) { item }.joinToString(",")}]}"""
	}

	private fun itemJson(origin: String, reporter: UUID): String =
		"""{"origin":"$origin","body":"hello from $origin","sha512":"abc123","moment":"1762963200.123456789","reporterUserGlobalId":"$reporter","reporterComment":"seen on device","calcedRank":0.5,"detectedDiagnostics":"Spam","expectedDiagnostics":"Useful","processorsReports":[{"processorName":"rule-a","processorVersion":"1.2","generalRank":0.25,"userOverrideRank":0.5,"processorProvidedComment":"kept","position":1},{"processorName":"rule-b","processorVersion":"3","generalRank":1.0,"userOverrideRank":null,"processorProvidedComment":"auto","position":2}]}"""

	private data class StoredReport(
		val id: Long,
		val globalId: UUID?,
		val origin: String,
		val body: String?,
		val sha512: String?,
		val reporterUserGlobalId: UUID?,
		val reporterComment: String?,
		val detectedDiagnostics: String,
		val expectedDiagnostics: String?,
	)

	private data class StoredResult(
		val reportId: Long,
		val processorName: String,
		val processorVersion: String,
		val generalRank: Float,
		val userOverrideRank: Float?,
		val processorProvidedComment: String,
		val position: Int,
	)

	private companion object {
		const val REPORTS = "/v1/public/payamak-report"
	}
}
