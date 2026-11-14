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

class PayamakReportProtectedServicePostponeTest : PayamakReportProtectedServiceTestBase() {
	@Test
	fun `postpone marks a pending report postponed and saves it`() {
		// Arrange
		val entity = report(state = PayamakReportStates.PENDING, flagged = false)
		given(entity)

		// Act
		val updated = runBlocking { service.postpone(entity.globalId) }

		// Assert
		assertEquals(PayamakReportStates.POSTPONED, updated.state)
		assertEquals(false, updated.flagged)
		runBlocking {
			verify(repo).findByGlobalId(entity.globalId)
			verify(repo).save(entity)
		}
		verifyNoInteractions(coreProvider, ruleResultRepo, entityTemplate, sorts)
	}

	@Test
	fun `postpone marks a reviewed report postponed without changing the decision`() {
		// Arrange
		val entity = report(
			state = PayamakReportStates.REVIEWED,
			flagged = true,
			reviewerComment = "later",
			decidedDiagnostics = PayamakReportDiagnostics.Spam,
		)
		given(entity)

		// Act
		val updated = runBlocking { service.postpone(entity.globalId) }

		// Assert
		assertEquals(PayamakReportStates.POSTPONED, updated.state)
		assertEquals("later", updated.reviewerComment)
		assertEquals(PayamakReportDiagnostics.Spam, updated.decidedDiagnostics)
		assertEquals(true, updated.flagged)
		runBlocking {
			verify(repo).findByGlobalId(entity.globalId)
			verify(repo).save(entity)
		}
		verifyNoInteractions(coreProvider, ruleResultRepo, entityTemplate, sorts)
	}

	@Test
	fun `postpone does not save when the report is already postponed`() {
		// Arrange
		val entity = report(state = PayamakReportStates.POSTPONED, flagged = true, reviewerComment = "waiting")
		given(entity)

		// Act
		val updated = runBlocking { service.postpone(entity.globalId) }

		// Assert
		assertEquals(PayamakReportStates.POSTPONED, updated.state)
		assertEquals("waiting", updated.reviewerComment)
		runBlocking {
			verify(repo).findByGlobalId(entity.globalId)
			verify(repo, never()).save(any())
		}
		verifyNoInteractions(coreProvider, ruleResultRepo, entityTemplate, sorts)
	}

	@Test
	fun `postpone rejects a missing report`() {
		// Arrange
		val missing = UUID.randomUUID()
		given(null)

		// Act
		val error = assertFailsWith<EntityNotFoundException> {
			runBlocking { service.postpone(missing) }
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
