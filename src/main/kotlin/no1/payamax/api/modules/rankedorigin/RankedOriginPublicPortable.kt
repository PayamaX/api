package no1.payamax.api.modules.rankedorigin

import java.util.UUID

data class RankedOriginPublicPortable(
	val identifier: Long,
	val globalId: UUID,
	val origin: String,
	val rank: Double,
	val position: Int,
	val comment: String?,
) {
	companion object {
		fun create(entity: RankedOriginEntity) = RankedOriginPublicPortable(
			entity.id ?: throw IllegalStateException("No id"),
			entity.globalId,
			entity.origin,
			entity.rank,
			entity.position,
			entity.comment,
		)
	}
}