package no1.payamax.api.modules.predefinedorigin

import java.util.UUID
import kotlin.time.Instant

data class PredefinedOriginPublicPortable(
	val globalId: UUID,
	val origin: String,
	val rank: Double,
	val order: Int,
	val added: Instant,
)