package no1.payamax.api.modules.predefinedorigin

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PredefinedOriginRepository : CoroutineCrudRepository<PredefinedOriginEntity, Long> {
	suspend fun findBy(pageable: Pageable): Flow<PredefinedOriginEntity>
	suspend fun findAllByOwnerGlobalIdNull(pageable: Pageable): Flow<PredefinedOriginEntity>
	suspend fun findAllByOwnerGlobalId(ownerId: UUID, pageable: Pageable): Flow<PredefinedOriginEntity>
	suspend fun countByOwnerGlobalIdNull(): Long
	suspend fun countByOwnerGlobalId(ownerId: UUID): Long
	suspend fun findOneByOrigin(origin: String): PredefinedOriginEntity?
}
