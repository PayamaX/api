package no1.payamax.api.modules.predefinedorigin

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface PredefinedOriginPublicContract {
	suspend fun paged(pageable: Pageable): Page<PredefinedOriginPublicPortable>
	suspend fun paged(pageable: Pageable, ownerId: UUID): Page<PredefinedOriginPublicPortable>
	suspend fun persist(input: PredefinedOriginProtectedInput): PredefinedOriginPublicPortable
}