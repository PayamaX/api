package no1.payamax.api.modules.rankedorigin

import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RankedOriginPublicService(
	private val coreProvider: RankedOriginCoreContract,
) : RankedOriginPublicContract {
	override suspend fun changes(lastSynced: Instant, cursor: RankedOriginCursor?): List<RankedOriginPublicPortable> {
		return coreProvider.changes(lastSynced, cursor).map { RankedOriginPublicPortable.create(it) }
	}
}