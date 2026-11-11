package no1.payamax.api.modules.rankedorigin

import no1.payamax.api.config.http.r2dbc.Sorts
import no1.payamax.api.config.http.r2dbc.toPage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class RankedOriginPublicService(
	private val coreProvider: RankedOriginCoreContract,
) : RankedOriginPublicContract {
	override suspend fun changes(lastSynced: Instant, cursor: RankedOriginCursor?): List<RankedOriginPublicPortable> {
		return coreProvider.changes(lastSynced, cursor).map { RankedOriginPublicPortable.create(it) }
	}
}