package no1.payamax.api.modules.rankedorigin

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface RankedOriginProtectedContract {
	suspend fun get(identifier: String): RankedOriginProtectedPortable
	suspend fun paged(pageable: Pageable): Page<RankedOriginProtectedPortable>
	suspend fun persist(input: RankedOriginProtectedInput): RankedOriginProtectedPortable
	suspend fun delete(globalId: UUID) : RankedOriginProtectedPortable
	suspend fun delete(globalIds: Array<UUID>)
}