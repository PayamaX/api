package no1.payamax.api.modules.rankedorigin

import io.swagger.v3.oas.annotations.Parameter
import kotlin.time.Instant

data class RankedOriginCursor(
	@field:Parameter(required = false) val updated: Instant,
	@field:Parameter(required = false) val identifier: Long,
)
