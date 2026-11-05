package no1.payamax.api.modules.predefinedorigin

import no1.payamax.api.r2dbc.toPage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class PredefinedOriginProtectedService(private val coreProvider: PredefinedOriginCoreService, private val repository: PredefinedOriginRepository) : PredefinedOriginProtectedContract {
	override suspend fun paged(pageable: Pageable): Page<PredefinedOriginProtectedPortable> {
		return repository.findBy(pageable).toPage(pageable, repository.count()).map { PredefinedOriginProtectedPortable.create(it) }
	}
}