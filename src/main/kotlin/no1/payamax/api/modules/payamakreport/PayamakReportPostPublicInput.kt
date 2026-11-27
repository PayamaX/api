package no1.payamax.api.modules.payamakreport

import jakarta.validation.constraints.Size

data class PayamakReportPostPublicInput(@Size(min = 1, max = 100) val items: List<PayamakReportPublicInput>)
