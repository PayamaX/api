package no1.payamax.api.modules.payamakreport

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PayamakReportRepository : CoroutineCrudRepository<PayamakReportEntity, Long> {
}