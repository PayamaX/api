package no1.payamax.api.modules.rankedorigin

import org.hibernate.validator.constraints.Length
import java.util.*

data class RankedOriginProtectedInput(
	val globalId: UUID?,
	@Length(max = 16) val origin: String,
	val rank: Float,
	val ownerGlobalId: UUID?,
	val position: Int,
	@Length(max = 512) val comment: String?,
)