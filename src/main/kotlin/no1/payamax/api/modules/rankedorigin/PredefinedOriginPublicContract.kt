package no1.payamax.api.modules.rankedorigin

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface RankedOriginPublicContract {
	suspend fun paged(pageable: Pageable): Page<RankedOriginPublicPortable>
	suspend fun paged(pageable: Pageable, ownerId: UUID): Page<RankedOriginPublicPortable>
}