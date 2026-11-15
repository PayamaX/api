package no1.payamax.api.modules.payamakreport.payamakreportruleprocessresult

import no1.payamax.api.exceptions.EntityNotFoundException
import no1.payamax.api.modules.payamakreport.PayamakReportEntity
import no1.payamax.api.modules.payamakreport.PayamakReportRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PayamakReportRuleProcessResultProtectedService(
	private val payamakReportRepository: PayamakReportRepository,
	private val payamakReportRuleProcessResultRepository: PayamakReportRuleProcessResultRepository,
) : PayamakReportRuleProcessResultProtectedContract {
	override suspend fun getByPayamakReportId(payamakReportGlobalId: UUID): List<PayamakReportRuleProcessResultProtectedPortable> {
		val payamakReport = payamakReportRepository.findByGlobalId(payamakReportGlobalId) ?: throw EntityNotFoundException(PayamakReportEntity::class.java, payamakReportGlobalId)
		val list = payamakReportRuleProcessResultRepository.findAllByReportId(payamakReport.id!!)
		return list.map { PayamakReportRuleProcessResultProtectedPortable.create(it) }
	}
}