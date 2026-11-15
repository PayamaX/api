package no1.payamax.api.modules.payamakreport.payamakreportruleprocessresult

import java.util.UUID

interface PayamakReportRuleProcessResultProtectedContract {
	suspend fun getByPayamakReportId(payamakReportGlobalId: UUID): List<PayamakReportRuleProcessResultProtectedPortable>
}