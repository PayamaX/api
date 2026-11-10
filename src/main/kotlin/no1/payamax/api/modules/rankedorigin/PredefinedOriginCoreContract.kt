package no1.payamax.api.modules.rankedorigin

interface RankedOriginCoreContract {
	suspend fun persist(input: RankedOriginProtectedInput): RankedOriginEntity
}