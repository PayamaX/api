package no1.payamax.api.modules.predefinedorigin

import java.util.*

data class PredefinedOriginProtectedInput(
	val id: Long,
	val globalId: UUID,
	val origin: String,
	val rank: Double,
	val ownerId: UUID?,
	val position: Int,
)