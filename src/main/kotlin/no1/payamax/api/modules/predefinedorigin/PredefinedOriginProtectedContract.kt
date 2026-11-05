package no1.payamax.api.modules.predefinedorigin

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PredefinedOriginProtectedContract {
	suspend fun paged(pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<no1.payamax.api.modules.predefinedorigin.PredefinedOriginProtectedPortable>

}