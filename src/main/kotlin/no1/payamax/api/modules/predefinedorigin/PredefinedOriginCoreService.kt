package no1.payamax.api.modules.predefinedorigin

import no1.payamax.api.exceptions.InvalidValueException
import org.springframework.stereotype.Service
import java.util.*

@Service
class PredefinedOriginCoreService(private val repository: PredefinedOriginRepository) : PredefinedOriginCoreContract {
	override suspend fun persist(input: PredefinedOriginProtectedInput): PredefinedOriginEntity {
		var persisted = repository.findOneByOrigin(input.origin)
		if (persisted != null) {
			if (persisted.globalId != input.globalId) {
				throw InvalidValueException()
			}
			persisted.origin = input.origin
			persisted.rank = input.rank
			persisted.ownerGlobalId = input.ownerGlobalId
			persisted.position = input.position
		} else {
			persisted = PredefinedOriginEntity(
				null,
				UUID.randomUUID(),
				input.origin,
				input.rank,
				input.ownerGlobalId,
				input.position
			)
		}
		return repository.save(persisted)
	}
}