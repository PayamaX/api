package no1.payamax.api.modules.payamakreport.payamakruleprocessresultreport

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PayamakRuleProcessResultReportRepository : CoroutineCrudRepository<PayamakRuleProcessResultReportEntity, Long> {
}