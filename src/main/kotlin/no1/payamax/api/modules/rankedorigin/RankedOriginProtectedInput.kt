package no1.payamax.api.modules.rankedorigin

import java.util.*

data class RankedOriginProtectedInput(
	val globalId: UUID?,
	val origin: String,
	val rank: Double,
	val ownerGlobalId: UUID?,
	val position: Int,
	val comment: String?,
)