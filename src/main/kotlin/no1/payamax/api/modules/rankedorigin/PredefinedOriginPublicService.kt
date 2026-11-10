package no1.payamax.api.modules.rankedorigin

import no1.payamax.api.config.http.r2dbc.Sorts
import no1.payamax.api.config.http.r2dbc.toPage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.*

@Service
class RankedOriginPublicService(
	private val coreProvider: RankedOriginCoreContract,
	private val repository: RankedOriginRepository,
	private val sorts: Sorts,
) : RankedOriginPublicContract {
	override suspend fun paged(pageable: Pageable): Page<RankedOriginPublicPortable> {
		sorts.require(pageable, RankedOriginEntity::class.java)
		return repository.findAllByOwnerGlobalIdNull(pageable)
			.toPage(pageable, repository.countByOwnerGlobalIdNull())
			.map { RankedOriginPublicPortable.create(it) }
	}

	override suspend fun paged(pageable: Pageable, ownerId: UUID): Page<RankedOriginPublicPortable> {
		sorts.require(pageable, RankedOriginEntity::class.java)
		return repository.findAllByOwnerGlobalId(ownerId, pageable)
			.toPage(pageable, repository.countByOwnerGlobalId(ownerId))
			.map { RankedOriginPublicPortable.create(it) }
	}
}