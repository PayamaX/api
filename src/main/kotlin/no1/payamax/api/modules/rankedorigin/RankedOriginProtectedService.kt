package no1.payamax.api.modules.rankedorigin

import kotlinx.coroutines.flow.collect
import no1.payamax.api.config.r2dbc.Sorts
import no1.payamax.api.config.r2dbc.toPage
import no1.payamax.api.exceptions.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class RankedOriginProtectedService(
	private val coreProvider: RankedOriginCoreContract,
	private val repository: RankedOriginRepository,
	private val sorts: Sorts,
) : RankedOriginProtectedContract {

	override suspend fun get(identifier: String): RankedOriginProtectedPortable {
		val globalId = runCatching { UUID.fromString(identifier) }.getOrNull()
		val entity = if (globalId != null) (repository.findOneByGlobalId(globalId) ?: throw EntityNotFoundException(RankedOriginEntity::class.java, globalId))
		else (repository.findOneByOrigin(identifier) ?: throw EntityNotFoundException(RankedOriginEntity::class.java, "origin", identifier))
		return RankedOriginProtectedPortable.create(entity)
	}

	override suspend fun paged(pageable: Pageable): Page<RankedOriginProtectedPortable> {
		sorts.require(pageable, RankedOriginEntity::class.java)
		return repository.findBy(pageable).toPage(pageable, repository.count()).map { RankedOriginProtectedPortable.create(it) }
	}

	override suspend fun persist(input: RankedOriginProtectedInput): RankedOriginProtectedPortable {
		return RankedOriginProtectedPortable.create(coreProvider.persist(RankedOriginCoreInput(input.globalId, input.origin, input.rank, input.ownerGlobalId, input.position, input.comment)))
	}

	override suspend fun delete(globalId: UUID): RankedOriginProtectedPortable {
		val now = Instant.now()
		val item = repository.findOneByGlobalId(globalId) ?: throw EntityNotFoundException(RankedOriginEntity::class.java, globalId)
		item.updated = now
		item.deleted = now
		repository.save(item)
		return RankedOriginProtectedPortable.create(item)
	}

	override suspend fun delete(globalIds: Array<UUID>) {
		val now = Instant.now()
		val items = repository.findAllByGlobalIdIn(globalIds)
		for (item in items) {
			item.updated = now
			item.deleted = now
		}
		repository.saveAll(items).collect()
	}
}