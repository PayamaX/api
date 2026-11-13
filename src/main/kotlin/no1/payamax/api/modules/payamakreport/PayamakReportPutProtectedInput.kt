package no1.payamax.api.modules.payamakreport

data class PayamakReportPutProtectedInput(
	val decidedDiagnostics: PayamakReportDiagnostics,
	val reviewerComment: String?,
)
