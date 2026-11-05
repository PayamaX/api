package no1.payamax.api.modules.predefinedorigin

import java.util.*

data class PredefinedOriginProtectedPortable(
	val id: Long,
	val globalId: UUID,
	val origin: String,
	val rank: Double,
	val ownerGlobalId: UUID?,
	val position: Int,
) {
	companion object {
		fun create(entity: PredefinedOriginEntity) = PredefinedOriginProtectedPortable(
			entity.id,
			entity.globalId,
			entity.origin,
			entity.rank,
			entity.ownerGlobalId,
			entity.position,
		)
	}
}