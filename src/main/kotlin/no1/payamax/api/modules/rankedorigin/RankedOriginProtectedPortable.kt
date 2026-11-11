package no1.payamax.api.modules.rankedorigin

import java.util.*

data class RankedOriginProtectedPortable(
	val localId: Long,
	val globalId: UUID,
	val origin: String,
	val rank: Double,
	val ownerGlobalId: UUID?,
	val position: Int,
	val comment: String?,
) {
	companion object {
		fun create(entity: RankedOriginEntity) = RankedOriginProtectedPortable(
			checkNotNull(entity.id),
			entity.globalId,
			entity.origin,
			entity.rank,
			entity.ownerGlobalId,
			entity.position,
			entity.comment,
		)
	}
}