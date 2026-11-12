package no1.payamax.api.modules.payamakreport

interface PayamakReportCoreContract {
	suspend fun persist(input: PayamakReportCoreInput)
}