package no1.payamax.api.modules.payamakreport.payamakreportruleprocessresult

import no1.payamax.api.modules.payamakreport.PayamakReportEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PayamakReportRuleProcessResultRepository : CoroutineCrudRepository<PayamakReportRuleProcessResultEntity, Long> {
	fun findAllByPayamakReport(payamakReport: PayamakReportEntity): List<PayamakReportRuleProcessResultEntity>
}