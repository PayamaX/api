package no1.payamax.api.modules.payamakreport.payamakreportruleprocessresult

import org.hibernate.validator.constraints.Length


data class PayamakReportRuleProcessResultPublicInput(
	@Length(max =  32) var processorName: String,
	var processorVersion: Int,
	var generalRank: Float,
	var userOverrideRank: Float?,
	@Length(max =  4096) var processorProvidedComment: String,
	var position: Int,
)
