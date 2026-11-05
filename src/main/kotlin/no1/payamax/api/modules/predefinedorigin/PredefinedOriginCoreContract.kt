package no1.payamax.api.modules.predefinedorigin

interface PredefinedOriginCoreContract {
	suspend fun persist(input: PredefinedOriginProtectedInput): PredefinedOriginEntity

}