package no1.payamax.api.modules.predefinedorigin

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PredefinedOriginPublicService(
	private val coreProvider: PredefinedOriginCoreContract,
	private val repository: PredefinedOriginRepository
) : PredefinedOriginPublicContract {
	override fun paged(pageable: Pageable): Page<PredefinedOriginPublicPortable> {
		return repository.findAllByOwnerGlobalIdNull(pageable).map { PredefinedOriginPublicPortable.create(it) }
	}

	override fun paged(pageable: Pageable, ownerId: UUID): Page<PredefinedOriginPublicPortable> {
		return repository.findAllByOwnerGlobalId(pageable, ownerId).map { PredefinedOriginPublicPortable.create(it) }
	}
}