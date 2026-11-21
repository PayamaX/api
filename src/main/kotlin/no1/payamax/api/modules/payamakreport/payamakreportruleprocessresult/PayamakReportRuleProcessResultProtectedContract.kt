package no1.payamax.api.modules.payamakreport.payamakreportruleprocessresult

import java.util.UUID

interface PayamakReportRuleProcessResultProtectedContract {
	suspend fun getByPayamakReportGlobalId(payamakReportGlobalId: UUID): List<PayamakReportRuleProcessResultProtectedPortable>
}