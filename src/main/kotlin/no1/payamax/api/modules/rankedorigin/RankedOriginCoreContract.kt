package no1.payamax.api.modules.rankedorigin

import kotlin.time.Instant

interface RankedOriginCoreContract {
	suspend fun persist(input: RankedOriginProtectedInput): RankedOriginEntity
	suspend fun changes(lastSynced: Instant, cursor: RankedOriginCursor?): List<RankedOriginEntity>
}