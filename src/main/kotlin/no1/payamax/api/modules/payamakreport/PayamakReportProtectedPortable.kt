package no1.payamax.api.modules.payamakreport

import no1.payamax.api.modules.payamakreport.payamakruleprocessresultreport.PayamakRuleProcessResultReportEntity
import no1.payamax.api.modules.payamakreport.payamakruleprocessresultreport.PayamakRuleProcessResultReportProtectedPortable
import java.util.UUID

class PayamakReportProtectedPortable(
	val id: Long? = null,
	val globalId: UUID,
	val state: PayamakReportStates,
	val origin: String,
	val body: String?,
	val sha512: String?,
	val reporterUserGlobalId: UUID?,
	val reporterComment: String?,
	val reviewerComment: String?,
	val flagged: Boolean,
	val detectedDiagnostics: PayamakReportDiagnostics,
	val expectedDiagnostics: PayamakReportDiagnostics?,
	val decidedDiagnostics: PayamakReportDiagnostics?,
) {
	companion object {
		fun create(entity: PayamakReportEntity) = PayamakReportProtectedPortable(
			entity.id,
			entity.globalId,
			entity.state,
			entity.origin,
			entity.body,
			entity.sha512,
			entity.reporterUserGlobalId,
			entity.reporterComment,
			entity.reviewerComment,
			entity.flagged,
			entity.detectedDiagnostics,
			entity.expectedDiagnostics,
			entity.decidedDiagnostics,
		)
	}
}