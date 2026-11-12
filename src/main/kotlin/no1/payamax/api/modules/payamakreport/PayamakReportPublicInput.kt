package no1.payamax.api.modules.payamakreport

import jakarta.validation.constraints.Size

data class PayamakReportPublicInput(@Size(min = 1, max = 100) val items: List<PayamakReportPublicItemInput>) {
	fun core(): PayamakReportCoreInput = PayamakReportCoreInput(items.map { PayamakReportCoreItemInput.core(it) })
}
