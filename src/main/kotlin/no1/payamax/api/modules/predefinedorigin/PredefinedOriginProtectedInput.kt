package no1.payamax.api.modules.predefinedorigin

import java.util.*

data class PredefinedOriginProtectedInput(
	val globalId: UUID?,
	val origin: String,
	val rank: Double,
	val ownerGlobalId: UUID?,
	val position: Int,
)