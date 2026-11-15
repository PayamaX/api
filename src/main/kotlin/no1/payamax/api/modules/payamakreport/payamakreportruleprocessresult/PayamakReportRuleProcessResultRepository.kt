package no1.payamax.api.modules.payamakreport.payamakreportruleprocessresult

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PayamakReportRuleProcessResultRepository : CoroutineCrudRepository<PayamakReportRuleProcessResultEntity, Long> {
	suspend fun findAllByReportId(reportId: Long): List<PayamakReportRuleProcessResultEntity>
}