package no1.payamax.api.modules.rankedorigin

import no1.payamax.api.config.http.r2dbc.Sorts
import no1.payamax.api.config.http.r2dbc.toPage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.stereotype.Service
import java.util.*
import kotlin.time.Instant

@Service
class RankedOriginPublicService(
	private val coreProvider: RankedOriginCoreContract,
	private val repository: RankedOriginRepository,
	private val sorts: Sorts,
) : RankedOriginPublicContract {
	override suspend fun changes(lastSynced: Instant, cursor: RankedOriginCursor?): List<RankedOriginPublicPortable> {
		return coreProvider.changes(lastSynced, cursor).map { RankedOriginPublicPortable.create(it) }
	}


	override suspend fun paged(pageable: Pageable, lastSynced: Instant): Page<RankedOriginPublicPortable> {
		sorts.require(pageable, RankedOriginEntity::class.java)
		return repository.findAllByOwnerGlobalIdNullAndUpdatedLessThan(pageable, lastSynced)
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