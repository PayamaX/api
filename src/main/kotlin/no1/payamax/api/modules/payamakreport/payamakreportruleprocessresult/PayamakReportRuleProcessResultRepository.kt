package no1.payamax.api.modules.payamakreport.payamakreportruleprocessresult

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PayamakReportRuleProcessResultRepository : CoroutineCrudRepository<PayamakReportRuleProcessResultEntity, Long> {
	suspend fun findAllByPayamakReportId(payamakReportId: Long): List<PayamakReportRuleProcessResultEntity>
	suspend fun findAllByPayamakReportIdIn(payamakReportIds: List<Long>): List<PayamakReportRuleProcessResultEntity>
}