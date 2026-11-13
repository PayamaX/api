package no1.payamax.api.modules.payamakreport

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PayamakReportRepository : CoroutineCrudRepository<PayamakReportEntity, Long> {
	fun findByGlobalId(globalId: UUID): PayamakReportEntity?
	fun findByGlobalIdIn(globalId: Array<UUID>): List<PayamakReportEntity>
}