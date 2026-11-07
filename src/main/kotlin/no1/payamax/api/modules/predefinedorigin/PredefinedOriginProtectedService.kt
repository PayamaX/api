package no1.payamax.api.modules.predefinedorigin

import no1.payamax.api.exceptions.EntityNotFoundException
import no1.payamax.api.r2dbc.Sorts
import no1.payamax.api.r2dbc.toPage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PredefinedOriginProtectedService(
	private val coreProvider: PredefinedOriginCoreService,
	private val repository: PredefinedOriginRepository,
	private val sorts: Sorts,
) : PredefinedOriginProtectedContract {
	override suspend fun paged(pageable: Pageable): Page<PredefinedOriginProtectedPortable> {
		sorts.require(pageable, PredefinedOriginEntity::class.java)
		return repository.findBy(pageable).toPage(pageable, repository.count()).map { PredefinedOriginProtectedPortable.create(it) }
	}

	override suspend fun persist(input: PredefinedOriginProtectedInput): PredefinedOriginProtectedPortable {
		return PredefinedOriginProtectedPortable.create(coreProvider.persist(input))
	}

	override suspend fun delete(globalId: UUID) {
		val persisted = repository.findOneByGlobalId(globalId) ?: throw EntityNotFoundException(PredefinedOriginEntity::class.java, globalId)
		repository.delete(persisted)
	}
}