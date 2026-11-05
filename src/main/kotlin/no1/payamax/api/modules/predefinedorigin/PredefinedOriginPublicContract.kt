package no1.payamax.api.modules.predefinedorigin

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface PredefinedOriginPublicContract {
	fun paged(pageable: Pageable): Page<PredefinedOriginPublicPortable>
	fun paged(pageable: Pageable, ownerId: UUID): Page<PredefinedOriginPublicPortable>
}