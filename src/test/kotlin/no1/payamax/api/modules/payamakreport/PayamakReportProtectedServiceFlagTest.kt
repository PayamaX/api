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

class PayamakReportProtectedServiceFlagTest : PayamakReportProtectedServiceTestBase() {
	@Test
	fun `flag marks a report and saves it`() {
		// Arrange
		val entity = report(flagged = false)
		given(entity)

		// Act
		val updated = runBlocking { service.flag(entity.globalId, true) }

		// Assert
		assertEquals(true, updated.flagged)
		assertEquals(PayamakReportStates.PENDING, updated.state)
		runBlocking {
			verify(repo).findByGlobalId(entity.globalId)
			verify(repo).save(entity)
		}
		verifyNoInteractions(coreProvider, ruleResultRepo, entityTemplate, sorts)
	}

	@Test
	fun `flag clears a report flag and saves it`() {
		// Arrange
		val entity = report(state = PayamakReportStates.REVIEWED, flagged = true, decidedDiagnostics = PayamakReportDiagnostics.Useful)
		given(entity)

		// Act
		val updated = runBlocking { service.flag(entity.globalId, false) }

		// Assert
		assertEquals(false, updated.flagged)
		assertEquals(PayamakReportStates.REVIEWED, updated.state)
		assertEquals(PayamakReportDiagnostics.Useful, updated.decidedDiagnostics)
		runBlocking {
			verify(repo).findByGlobalId(entity.globalId)
			verify(repo).save(entity)
		}
		verifyNoInteractions(coreProvider, ruleResultRepo, entityTemplate, sorts)
	}

	@Test
	fun `flag does not save when the flag already matches`() {
		// Arrange
		val entity = report(flagged = true, reviewerComment = "untouched")
		given(entity)

		// Act
		val updated = runBlocking { service.flag(entity.globalId, true) }

		// Assert
		assertEquals(true, updated.flagged)
		assertEquals("untouched", updated.reviewerComment)
		runBlocking {
			verify(repo).findByGlobalId(entity.globalId)
			verify(repo, never()).save(any())
		}
		verifyNoInteractions(coreProvider, ruleResultRepo, entityTemplate, sorts)
	}

	@Test
	fun `flag rejects a missing report`() {
		// Arrange
		val missing = UUID.randomUUID()
		given(null)

		// Act
		val error = assertFailsWith<EntityNotFoundException> {
			runBlocking { service.flag(missing, true) }
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
