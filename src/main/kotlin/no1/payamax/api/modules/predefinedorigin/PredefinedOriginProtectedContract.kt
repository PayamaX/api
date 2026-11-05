package no1.payamax.api.modules.predefinedorigin

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PredefinedOriginProtectedContract {
	suspend fun paged(pageable: Pageable): Page<PredefinedOriginProtectedPortable>
	suspend fun persist(input: PredefinedOriginProtectedInput): PredefinedOriginProtectedPortable
	suspend fun delete(globalId: java.util.UUID)
}