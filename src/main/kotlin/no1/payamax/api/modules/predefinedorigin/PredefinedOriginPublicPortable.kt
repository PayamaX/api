package no1.payamax.api.modules.predefinedorigin

import java.util.UUID
import kotlin.time.Instant

data class PredefinedOriginPublicPortable(
	val globalId: UUID,
	val origin: String,
	val rank: Double,
	val position: Int,
) {
	companion object {
		fun create(entity: PredefinedOriginEntity) = PredefinedOriginPublicPortable(entity.globalId, entity.origin, entity.rank, entity.position)
	}
}