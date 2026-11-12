package no1.payamax.api.modules.payamakreport

import org.springframework.stereotype.Service

@Service
class PayamakReportPublicService(private val payamakReportCoreProvider: PayamakReportCoreContract) : PayamakReportPublicContract {
	override suspend fun persist(input: PayamakReportPublicInput) {
		payamakReportCoreProvider.persist(input.core())
	}
}