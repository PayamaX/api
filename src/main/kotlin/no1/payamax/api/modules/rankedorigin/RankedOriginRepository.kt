package no1.payamax.api.modules.rankedorigin

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*
import kotlin.time.Instant

@Repository
interface RankedOriginRepository : CoroutineCrudRepository<RankedOriginEntity, Long> {
	suspend fun findBy(pageable: Pageable): Flow<RankedOriginEntity>
	suspend fun findAllByOwnerGlobalIdNullAndUpdatedLessThan(pageable: Pageable, updated: Instant): Flow<RankedOriginEntity>
	suspend fun findAllByOwnerGlobalId(ownerId: UUID, pageable: Pageable): Flow<RankedOriginEntity>
	suspend fun countByOwnerGlobalIdNull(): Long
	suspend fun countByOwnerGlobalId(ownerId: UUID): Long
	suspend fun findOneByOrigin(origin: String): RankedOriginEntity?
	suspend fun findOneByGlobalId(globalId: UUID): RankedOriginEntity?
	suspend fun findAllByGlobalIdIn(globalIds: Array<UUID>): List<RankedOriginEntity>
}
