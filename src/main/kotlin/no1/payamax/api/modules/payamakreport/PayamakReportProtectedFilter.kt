package no1.payamax.api.modules.payamakreport

import java.util.UUID

data class PayamakReportProtectedFilter(
	val state: List<PayamakReportStates>?,
	val globalId: UUID? = null,
	val origin: String? = null,
	val flagged: Boolean = false,
)
