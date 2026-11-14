package no1.payamax.api.modules.payamakreport

import kotlinx.coroutines.runBlocking
import no1.payamax.api.config.r2dbc.Sorts
import no1.payamax.api.modules.payamakreport.payamakruleprocessresultreport.PayamakRuleProcessResultReportRepository
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import java.util.UUID

abstract class PayamakReportProtectedServiceTestBase {
	protected val coreProvider: PayamakReportCoreContract = mock()
	protected val repo: PayamakReportRepository = mock()
	protected val ruleResultRepo: PayamakRuleProcessResultReportRepository = mock()
	protected val entityTemplate: R2dbcEntityTemplate = mock()
	protected val sorts: Sorts = mock()
	protected val service = PayamakReportProtectedService(
		coreProvider = coreProvider,
		repo = repo,
		ruleResultRepo = ruleResultRepo,
		entityTemplate = entityTemplate,
		sorts = sorts,
	)

	protected fun given(entity: PayamakReportEntity?) {
		runBlocking {
			whenever(repo.findByGlobalId(any())).thenReturn(entity)
			whenever(repo.save(any<PayamakReportEntity>())).thenAnswer { invocation ->
				invocation.getArgument<PayamakReportEntity>(0)
			}
		}
		clearInvocations(repo)
	}

	protected fun report(
		state: PayamakReportStates = PayamakReportStates.PENDING,
		flagged: Boolean = false,
		reviewerComment: String? = null,
		decidedDiagnostics: PayamakReportDiagnostics? = null,
	): PayamakReportEntity = PayamakReportEntity(
		id = 1L,
		globalId = UUID.randomUUID(),
		state = state,
		origin = "PayamaX",
		body = "hello",
		sha512 = null,
		reporterUserGlobalId = null,
		reporterComment = null,
		reviewerComment = reviewerComment,
		flagged = flagged,
		detectedDiagnostics = PayamakReportDiagnostics.Spam,
		expectedDiagnostics = null,
		decidedDiagnostics = decidedDiagnostics,
	)
}
