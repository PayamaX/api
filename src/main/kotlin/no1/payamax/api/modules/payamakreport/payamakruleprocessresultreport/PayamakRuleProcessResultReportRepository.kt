package no1.payamax.api.modules.payamakreport.payamakruleprocessresultreport

import no1.payamax.api.modules.payamakreport.PayamakReportEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PayamakRuleProcessResultReportRepository : CoroutineCrudRepository<PayamakRuleProcessResultReportEntity, Long> {
	fun findAllByPayamakReport(payamakReport: PayamakReportEntity): List<PayamakRuleProcessResultReportEntity>
}