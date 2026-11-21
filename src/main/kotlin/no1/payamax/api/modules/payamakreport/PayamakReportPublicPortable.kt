package no1.payamax.api.modules.payamakreport

import java.util.UUID

class PayamakReportPublicPortable(
	val globalId: UUID,
	val origin: String,
	val body: String?,
	val sha512: String?,
	val reporterComment: String?,
	val detectedDiagnostics: PayamakReportDiagnostics,
	val expectedDiagnostics: PayamakReportDiagnostics?,
) {
}