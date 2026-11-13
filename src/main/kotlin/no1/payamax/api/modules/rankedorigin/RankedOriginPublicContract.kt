package no1.payamax.api.modules.rankedorigin

import java.time.Instant

interface RankedOriginPublicContract {
	suspend fun changes(lastSynced: Instant, cursor: RankedOriginCursor?): List<RankedOriginPublicPortable>
}