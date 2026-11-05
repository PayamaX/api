package no1.payamax.api.modules.predefinedorigin

import no1.payamax.api.r2dbc.toPage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.*

@Service
class PredefinedOriginPublicService(
	private val coreProvider: PredefinedOriginCoreContract,
	private val repository: PredefinedOriginRepository,
) : PredefinedOriginPublicContract {
	override suspend fun paged(pageable: Pageable): Page<PredefinedOriginPublicPortable> {
		return repository.findAllByOwnerGlobalIdNull(pageable)
			.toPage(pageable, repository.countByOwnerGlobalIdNull())
			.map { PredefinedOriginPublicPortable.create(it) }
	}

	override suspend fun paged(pageable: Pageable, ownerId: UUID): Page<PredefinedOriginPublicPortable> {
		return repository.findAllByOwnerGlobalId(ownerId, pageable)
			.toPage(pageable, repository.countByOwnerGlobalId(ownerId))
			.map { PredefinedOriginPublicPortable.create(it) }
	}
}