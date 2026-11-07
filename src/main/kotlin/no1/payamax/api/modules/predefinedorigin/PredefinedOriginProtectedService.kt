package no1.payamax.api.modules.predefinedorigin

import no1.payamax.api.config.http.r2dbc.Sorts
import no1.payamax.api.config.http.r2dbc.toPage
import no1.payamax.api.exceptions.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.*

@Service
class PredefinedOriginProtectedService(
	private val coreProvider: PredefinedOriginCoreService,
	private val repository: PredefinedOriginRepository,
	private val sorts: Sorts,
) : PredefinedOriginProtectedContract {
	override suspend fun get(globalId: UUID): PredefinedOriginProtectedPortable {
		return repository.findOneByGlobalId(globalId)?.let { PredefinedOriginProtectedPortable.create(it) } ?: throw EntityNotFoundException(PredefinedOriginEntity::class.java, globalId)
	}

	override suspend fun paged(pageable: Pageable): Page<PredefinedOriginProtectedPortable> {
		sorts.require(pageable, PredefinedOriginEntity::class.java)
		return repository.findBy(pageable).toPage(pageable, repository.count()).map { PredefinedOriginProtectedPortable.create(it) }
	}

	override suspend fun persist(input: PredefinedOriginProtectedInput): PredefinedOriginProtectedPortable {
		return PredefinedOriginProtectedPortable.create(coreProvider.persist(input))
	}

	override suspend fun delete(globalId: UUID): PredefinedOriginProtectedPortable {
		val persisted = repository.findOneByGlobalId(globalId) ?: throw EntityNotFoundException(PredefinedOriginEntity::class.java, globalId)
		repository.delete(persisted)
		return PredefinedOriginProtectedPortable.create(persisted)
	}

	override suspend fun delete(globalIds: Array<UUID>) {
		repository.deleteAll(repository.findAllByGlobalIdIn(globalIds))
	}
}