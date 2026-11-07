package no1.payamax.api.modules.predefinedorigin

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface PredefinedOriginProtectedContract {
	suspend fun get(globalId: UUID): PredefinedOriginProtectedPortable
	suspend fun paged(pageable: Pageable): Page<PredefinedOriginProtectedPortable>
	suspend fun persist(input: PredefinedOriginProtectedInput): PredefinedOriginProtectedPortable
	suspend fun delete(globalId: UUID) : PredefinedOriginProtectedPortable
	suspend fun delete(globalIds: Array<UUID>)
}