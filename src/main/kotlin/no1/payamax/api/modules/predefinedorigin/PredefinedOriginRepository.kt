package no1.payamax.api.modules.predefinedorigin

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PredefinedOriginRepository : JpaRepository<PredefinedOriginEntity, Long> {
	fun findAllByOwnerGlobalIdNull(pageable: Pageable): Page<PredefinedOriginEntity>
	fun findAllByOwnerGlobalId(pageable: Pageable, ownerId: UUID): Page<PredefinedOriginEntity>
}