package no1.payamax.api.modules.payamakreport

import jakarta.transaction.Transactional
import no1.payamax.api.modules.payamakreport.payamakruleprocessresultreport.PayamakRuleProcessResultReportEntity
import no1.payamax.api.modules.payamakreport.payamakruleprocessresultreport.PayamakRuleProcessResultReportRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PayamakReportCoreService(
	private val repo: PayamakReportRepository,
	private val ruleReportRepo: PayamakRuleProcessResultReportRepository,
) : PayamakReportCoreContract {

	@Transactional
	override suspend fun persist(input: PayamakReportCoreInput) {
		for (item in input.items) {
			val entity = PayamakReportEntity(
				null,
				UUID.randomUUID(),
				PayamakReportStates.PENDING,
				item.origin,
				item.body,
				item.sha512,
				item.reporterUserGlobalId,
				item.reporterComment,
				null,
				false,
				item.detectedDiagnostics,
				item.expectedDiagnostics,
				null,
			)
			val saved = repo.save(entity)
			for (proc in item.processorsReports) {
				val entity2 = PayamakRuleProcessResultReportEntity(
					null,
					UUID.randomUUID(),
					saved.id,
					saved,
					proc.processorName,
					proc.processorVersion,
					proc.generalRank,
					proc.userOverrideRank,
					proc.processorProvidedComment,
					proc.position
				)
				ruleReportRepo.save(entity2)
			}
		}
	}
}