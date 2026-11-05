package no1.payamax.api.modules.predefinedorigin

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class PredefinedOriginProtectedService(private val coreProvider: PredefinedOriginCoreService, private val repository: PredefinedOriginRepository) : PredefinedOriginProtectedContract {
	override fun paged(pageable: Pageable): Page<PredefinedOriginProtectedPortable> {
		return repository.findAll(pageable).map { PredefinedOriginProtectedPortable.create(it) }
	}
}