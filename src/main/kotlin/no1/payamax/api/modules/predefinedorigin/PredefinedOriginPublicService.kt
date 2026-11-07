package no1.payamax.api.modules.predefinedorigin

import no1.payamax.api.config.http.r2dbc.Sorts
import no1.payamax.api.config.http.r2dbc.toPage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.*

@Service
class PredefinedOriginPublicService(
	private val coreProvider: PredefinedOriginCoreContract,
	private val repository: PredefinedOriginRepository,
	private val sorts: Sorts,
) : PredefinedOriginPublicContract {
	override suspend fun paged(pageable: Pageable): Page<PredefinedOriginPublicPortable> {
		sorts.require(pageable, PredefinedOriginEntity::class.java)
		return repository.findAllByOwnerGlobalIdNull(pageable)
			.toPage(pageable, repository.countByOwnerGlobalIdNull())
			.map { PredefinedOriginPublicPortable.create(it) }
	}

	override suspend fun paged(pageable: Pageable, ownerId: UUID): Page<PredefinedOriginPublicPortable> {
		sorts.require(pageable, PredefinedOriginEntity::class.java)
		return repository.findAllByOwnerGlobalId(ownerId, pageable)
			.toPage(pageable, repository.countByOwnerGlobalId(ownerId))
			.map { PredefinedOriginPublicPortable.create(it) }
	}
}