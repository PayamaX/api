package no1.payamax.api.modules.payamakreport

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import no1.payamax.api.modules.payamakreport.payamakruleprocessresultreport.PayamakRuleProcessResultReportPublicInput
import org.hibernate.validator.constraints.Length
import java.time.Instant
import java.util.*

data class PayamakReportPublicItemInput(
	@NotNull val origin: String,
	val body: String?,
	val sha512: String?,
	@NotNull val moment: Instant,
	val reporterUserGlobalId: UUID?,
	@NotNull @Length(max = 255) val reporterComment: String?,
	@DecimalMin("0.0") @DecimalMax("1.0") val calcedRank: Float,
	@NotNull val detectedDiagnostics: PayamakReportDiagnostics,
	@NotNull val expectedDiagnostics: PayamakReportDiagnostics?,
	@NotEmpty val processorsReports: Array<PayamakRuleProcessResultReportPublicInput>,
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as PayamakReportPublicItemInput

		if (calcedRank != other.calcedRank) return false
		if (origin != other.origin) return false
		if (body != other.body) return false
		if (sha512 != other.sha512) return false
		if (moment != other.moment) return false
		if (reporterUserGlobalId != other.reporterUserGlobalId) return false
		if (reporterComment != other.reporterComment) return false
		if (detectedDiagnostics != other.detectedDiagnostics) return false
		if (expectedDiagnostics != other.expectedDiagnostics) return false
		if (!processorsReports.contentEquals(other.processorsReports)) return false

		return true
	}

	override fun hashCode(): Int {
		var result = calcedRank.hashCode()
		result = 31 * result + origin.hashCode()
		result = 31 * result + body.hashCode()
		result = 31 * result + sha512.hashCode()
		result = 31 * result + moment.hashCode()
		result = 31 * result + reporterUserGlobalId.hashCode()
		result = 31 * result + reporterComment.hashCode()
		result = 31 * result + detectedDiagnostics.hashCode()
		result = 31 * result + expectedDiagnostics.hashCode()
		result = 31 * result + processorsReports.contentHashCode()
		return result
	}
}