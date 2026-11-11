package no1.payamax.api.modules.rankedorigin

import java.time.Instant


interface RankedOriginCoreContract {
	suspend fun persist(input: RankedOriginCoreInput): RankedOriginEntity
	suspend fun changes(lastSynced: Instant, cursor: RankedOriginCursor?): List<RankedOriginEntity>
}