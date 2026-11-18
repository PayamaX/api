package no1.payamax.api.modules.payamakreport.payamakreportruleprocessresult

import java.util.UUID

data class PayamakReportRuleProcessResultProtectedPortable(
	val identifier: Long?,
	val globalId: UUID,
	val reportId: Long?,
	val processorName: String,
	val processorVersion: String,
	val generalRank: Float,
	val userOverrideRank: Float?,
	val processorProvidedComment: String?,
	val position: Int,
) {
	companion object {
		fun create(entity: PayamakReportRuleProcessResultEntity): PayamakReportRuleProcessResultProtectedPortable = PayamakReportRuleProcessResultProtectedPortable(
			entity.id,
			entity.globalId,
			entity.payamakReportId,
			entity.processorName,
			entity.processorVersion,
			entity.generalRank,
			entity.userOverrideRank,
			entity.processorProvidedComment,
			entity.position,
		)
	}
}