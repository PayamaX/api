package no1.payamax.api.modules.payamakreport

interface PayamakReportPublicContract {
	suspend fun persist(input: PayamakReportPublicInput)
}