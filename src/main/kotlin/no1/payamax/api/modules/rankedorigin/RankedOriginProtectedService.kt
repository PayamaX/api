package no1.payamax.api.modules.rankedorigin

import no1.payamax.api.config.http.r2dbc.Sorts
import no1.payamax.api.config.http.r2dbc.toPage
import no1.payamax.api.exceptions.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.*

@Service
class RankedOriginProtectedService(
	private val coreProvider: RankedOriginCoreService,
	private val repository: RankedOriginRepository,
	private val sorts: Sorts,
) : RankedOriginProtectedContract {
	override suspend fun get(globalId: UUID): RankedOriginProtectedPortable {
		return repository.findOneByGlobalId(globalId)?.let { RankedOriginProtectedPortable.create(it) } ?: throw EntityNotFoundException(RankedOriginEntity::class.java, globalId)
	}

	override suspend fun paged(pageable: Pageable): Page<RankedOriginProtectedPortable> {
		sorts.require(pageable, RankedOriginEntity::class.java)
		return repository.findBy(pageable).toPage(pageable, repository.count()).map { RankedOriginProtectedPortable.create(it) }
	}

	override suspend fun persist(input: RankedOriginProtectedInput): RankedOriginProtectedPortable {
		return RankedOriginProtectedPortable.create(coreProvider.persist(input))
	}

	override suspend fun delete(globalId: UUID): RankedOriginProtectedPortable {
		val persisted = repository.findOneByGlobalId(globalId) ?: throw EntityNotFoundException(RankedOriginEntity::class.java, globalId)
		repository.delete(persisted)
		return RankedOriginProtectedPortable.create(persisted)
	}

	override suspend fun delete(globalIds: Array<UUID>) {
		repository.deleteAll(repository.findAllByGlobalIdIn(globalIds))
	}
}