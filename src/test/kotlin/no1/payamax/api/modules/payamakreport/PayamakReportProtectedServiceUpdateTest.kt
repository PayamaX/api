package no1.payamax.api.modules.payamakreport

import kotlinx.coroutines.runBlocking
import no1.payamax.api.exceptions.EntityNotFoundException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.http.HttpStatus
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PayamakReportProtectedServiceUpdateTest : PayamakReportProtectedServiceTestBase() {
	@Test
	fun `update stores the decision and marks the report reviewed`() {
		// Arrange
		val entity = report(state = PayamakReportStates.POSTPONED, flagged = true)
		given(entity)

		// Act
		val updated = runBlocking {
			service.update(entity.globalId, PayamakReportPutProtectedInput(PayamakReportDiagnostics.Useful, "kept"))
		}

		// Assert
		assertEquals(PayamakReportStates.REVIEWED, updated.state)
		assertEquals(PayamakReportDiagnostics.Useful, updated.decidedDiagnostics)
		assertEquals("kept", updated.reviewerComment)
		assertEquals(true, updated.flagged)
		runBlocking {
			verify(repo).findByGlobalId(entity.globalId)
			verify(repo).save(entity)
		}
		verifyNoInteractions(coreProvider, ruleResultRepo, entityTemplate, sorts)
	}

	@Test
	fun `update clears the reviewer comment`() {
		// Arrange
		val entity = report(reviewerComment = "old note", decidedDiagnostics = PayamakReportDiagnostics.Spam)
		given(entity)

		// Act
		val updated = runBlocking {
			service.update(entity.globalId, PayamakReportPutProtectedInput(PayamakReportDiagnostics.Important, null))
		}

		// Assert
		assertEquals(null, updated.reviewerComment)
		assertEquals(PayamakReportDiagnostics.Important, updated.decidedDiagnostics)
		assertEquals(PayamakReportStates.REVIEWED, entity.state)
		runBlocking {
			verify(repo).findByGlobalId(entity.globalId)
			verify(repo).save(entity)
		}
		verifyNoInteractions(coreProvider, ruleResultRepo, entityTemplate, sorts)
	}

	@Test
	fun `update rejects a missing report`() {
		// Arrange
		val missing = UUID.randomUUID()
		given(null)

		// Act
		val error = assertFailsWith<EntityNotFoundException> {
			runBlocking { service.update(missing, PayamakReportPutProtectedInput(PayamakReportDiagnostics.Spam, null)) }
		}

		// Assert
		assertEquals(HttpStatus.BAD_REQUEST, error.statusCode)
		runBlocking {
			verify(repo).findByGlobalId(missing)
			verify(repo, never()).save(any())
		}
		verifyNoInteractions(coreProvider, ruleResultRepo, entityTemplate, sorts)
	}
}
