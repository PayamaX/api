package no1.payamax.api.modules.payamakreport

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import no1.payamax.api.modules.payamakreport.payamakreportruleprocessresult.PayamakReportRuleProcessResultPublicInput
import org.hibernate.validator.constraints.Length
import java.time.Instant
import java.util.*

data class PayamakReportPublicInput(
	@Length(max = 32) @NotNull val origin: String,
	@Length(max = 4096) val body: String?,
	@Length(min = 128, max = 128) val sha512: String?,
	@NotNull val moment: Instant,
	@NotNull @Length(max = 255) val reporterComment: String?,
	@DecimalMin("0.0") @DecimalMax("1.0") val calcedRank: Float,
	@NotNull val detectedDiagnostics: PayamakReportDiagnostics,
	@NotNull val expectedDiagnostics: PayamakReportDiagnostics?,
	@NotEmpty val processorsReports: List<PayamakReportRuleProcessResultPublicInput>,
)