package no1.payamax.api.modules.payamakreport.payamakruleprocessresultreport

data class PayamakRuleProcessResultReportPublicInput(
	var processorName: String,
	var processorVersion: String,
	var generalRank: Float,
	var userOverrideRank: Float,
	var processorProvidedComment: String,
	var position: Int,
)
