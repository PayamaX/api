package no1.payamax.api.modules.payamakreport

import java.util.UUID

class PayamakReportProtectedPortable(
	val id: Long? = null,
	val globalId: UUID,
	val state: PayamakReportStates,
	val origin: String,
	val body: String?,
	val sha512: String?,
	val reporterUserGlobalId: UUID?,
	val reporterComment: String?,
	val reviewerComment: String?,
	val flagged: Boolean,
	val detectedDiagnostics: PayamakReportDiagnostics,
	val expectedDiagnostics: PayamakReportDiagnostics?,
	val decidedDiagnostics: PayamakReportDiagnostics?,
) {
}