package no1.payamax.api.modules.rankedorigin

import java.util.*

data class RankedOriginProtectedInput(
	val globalId: UUID?,
	val origin: String,
	val rank: Float,
	val ownerGlobalId: UUID?,
	val position: Int,
	val comment: String?,
)