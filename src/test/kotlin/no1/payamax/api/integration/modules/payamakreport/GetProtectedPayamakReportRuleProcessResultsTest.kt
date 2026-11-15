package no1.payamax.api.integration.modules.payamakreport

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class GetProtectedPayamakReportRuleProcessResultsTest : PayamakReportProtectedV1TestBase() {
	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `get returns the rule process results for that report`() {
		// Arrange
		val (reportGlobalId, results) = runBlocking {
			val reportGlobalId = insert()
			val reportId = reportId(reportGlobalId)
			val otherReportId = reportId(insert(origin = "Other"))
			insertResult(otherReportId, processorName = "other", processorVersion = "9", generalRank = 0.1f, userOverrideRank = null, comment = "ignored", position = 1)
			reportGlobalId to listOf(
				insertResult(reportId, processorName = "rule-a", processorVersion = "1.2", generalRank = 0.25f, userOverrideRank = 0.5f, comment = "kept", position = 1),
				insertResult(reportId, processorName = "rule-b", processorVersion = "3", generalRank = 1.0f, userOverrideRank = null, comment = "auto", position = 2),
			)
		}

		// Act
		val response = getResults(reportGlobalId)

		// Assert
		response.expectOk(results)
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `get returns an empty list when the report has no results`() {
		// Arrange
		val reportGlobalId = runBlocking { insert() }

		// Act
		val response = getResults(reportGlobalId)

		// Assert
		response.expectOk(emptyList())
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `get returns bad request when the report is missing`() {
		// Arrange
		val missing = UUID.randomUUID()

		// Act
		val response = getResults(missing)

		// Assert
		response.expectMissing(RESULTS)
	}

	@Test
	@WithMockUser(roles = ["ADMIN"])
	fun `get rejects a report id that is not a uuid`() {
		// Arrange
		val request = get(RESULTS)
			.param("payamakReportGlobalId", "not-a-uuid")
			.accept(MediaType.APPLICATION_JSON)

		// Act
		val response = exchange(request)

		// Assert
		response
			.andExpect(status().isBadRequest)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.title").value("Bad Request"))
			.andExpect(jsonPath("$.detail", containsString("payamakReportGlobalId")))
	}

	@Test
	@WithMockUser(roles = ["USER"])
	fun `get rejects a caller without ROLE_ADMIN`() {
		// Arrange
		val reportGlobalId = UUID.randomUUID()

		// Act
		val response = getResults(reportGlobalId)

		// Assert
		response.expectForbidden()
	}

	@Test
	fun `get rejects a caller with no credentials`() {
		// Arrange
		// No token and no authenticated test user.

		// Act
		val response = getResults(UUID.randomUUID())

		// Assert
		response.expectForbidden()
	}

	@Test
	fun `get rejects an untrusted audience`() {
		// Arrange
		val token = untrustedToken()

		// Act
		val response = getResults(UUID.randomUUID(), token = token)

		// Assert
		response.andExpect(status().isUnauthorized)
	}

	@Test
	fun `get rejects a malformed token`() {
		// Arrange
		val token = "not-a-jwt"

		// Act
		val response = getResults(UUID.randomUUID(), token = token)

		// Assert
		response.andExpect(status().isUnauthorized)
	}

	private fun getResults(payamakReportGlobalId: UUID, token: String? = null): ResultActions {
		val request = get(RESULTS)
			.param("payamakReportGlobalId", payamakReportGlobalId.toString())
			.accept(MediaType.APPLICATION_JSON)
		return exchange(withToken(request, token))
	}

	private fun ResultActions.expectOk(results: List<StoredResult>): ResultActions {
		var checks = andExpect(status().isOk)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.length()").value(results.size))
		results.forEach { result ->
			val match = "$[?(@.globalId == '${result.globalId}')]"
			checks = checks
				.andExpect(jsonPath("$match.id", contains(result.id.toInt())))
				.andExpect(jsonPath("$match.reportId", contains(result.reportId.toInt())))
				.andExpect(jsonPath("$match.processorName", contains(result.processorName)))
				.andExpect(jsonPath("$match.processorVersion", contains(result.processorVersion)))
				.andExpect(jsonPath("$match.generalRank", contains(result.generalRank.toDouble())))
				.andExpect(jsonPath("$match.processorProvidedComment", contains(result.processorProvidedComment)))
				.andExpect(jsonPath("$match.position", contains(result.position)))
			checks = if (result.userOverrideRank == null) {
				checks.andExpect(jsonPath("$match.userOverrideRank", contains(nullValue())))
			} else {
				checks.andExpect(jsonPath("$match.userOverrideRank", contains(result.userOverrideRank.toDouble())))
			}
		}
		return checks
	}

	private suspend fun reportId(globalId: UUID): Long {
		return databaseClient.sql(
			"""
			SELECT id
			FROM payamak_report
			WHERE global_id = :globalId
			""".trimIndent(),
		)
			.bind("globalId", globalId)
			.map { row, _ -> (row.get("id") as Number).toLong() }
			.one()
			.awaitSingle()
	}

	private suspend fun insertResult(
		reportId: Long,
		processorName: String,
		processorVersion: String,
		generalRank: Float,
		userOverrideRank: Float?,
		comment: String,
		position: Int,
	): StoredResult {
		val globalId = UUID.randomUUID()
		var spec = databaseClient.sql(
			"""
			INSERT INTO payamak_report_rule_process_result (
				global_id, payamak_report_id, processor_name, processor_version, general_rank,
				user_override_rank, processor_provided_comment, position
			) VALUES (
				:globalId, :reportId, :processorName, :processorVersion, :generalRank,
				:userOverrideRank, :processorProvidedComment, :position
			)
			RETURNING id
			""".trimIndent(),
		)
			.bind("globalId", globalId)
			.bind("reportId", reportId)
			.bind("processorName", processorName)
			.bind("processorVersion", processorVersion)
			.bind("generalRank", generalRank)
			.bind("processorProvidedComment", comment)
			.bind("position", position)
		spec = if (userOverrideRank == null) {
			spec.bindNull("userOverrideRank", Float::class.javaObjectType)
		} else {
			spec.bind("userOverrideRank", userOverrideRank)
		}
		val id = spec
			.map { row, _ -> (row.get("id") as Number).toLong() }
			.one()
			.awaitSingle()
		return StoredResult(
			id = id,
			globalId = globalId,
			reportId = reportId,
			processorName = processorName,
			processorVersion = processorVersion,
			generalRank = generalRank,
			userOverrideRank = userOverrideRank,
			processorProvidedComment = comment,
			position = position,
		)
	}

	private data class StoredResult(
		val id: Long,
		val globalId: UUID,
		val reportId: Long,
		val processorName: String,
		val processorVersion: String,
		val generalRank: Float,
		val userOverrideRank: Float?,
		val processorProvidedComment: String,
		val position: Int,
	)

	private companion object {
		const val RESULTS = "/v1/protected/payamak-report/rule-process-results"
	}
}
