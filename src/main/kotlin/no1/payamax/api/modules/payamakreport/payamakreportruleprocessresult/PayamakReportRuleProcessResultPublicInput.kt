package no1.payamax.api.modules.payamakreport.payamakreportruleprocessresult

data class PayamakReportRuleProcessResultPublicInput(
	var processorName: String,
	var processorVersion: String,
	var generalRank: Float,
	var userOverrideRank: Float?,
	var processorProvidedComment: String,
	var position: Int,
)
