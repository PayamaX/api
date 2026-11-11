package no1.payamax.api.modules.rankedorigin

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID
import kotlin.time.Instant

interface RankedOriginPublicContract {
	suspend fun changes(lastSynced: Instant, cursor: RankedOriginCursor?): List<RankedOriginPublicPortable>
	suspend fun paged(pageable: Pageable, lastSynced: Instant): Page<RankedOriginPublicPortable>
	suspend fun paged(pageable: Pageable, ownerId: UUID): Page<RankedOriginPublicPortable>
}