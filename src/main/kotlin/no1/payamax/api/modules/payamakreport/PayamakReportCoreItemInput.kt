package no1.payamax.api.modules.payamakreport

import no1.payamax.api.modules.payamakreport.payamakruleprocessresultreport.PayamakRuleProcessResultReportPublicInput
import java.time.Instant
import java.util.*

data class PayamakReportCoreItemInput(
	val origin: String,
	val body: String?,
	val hash: String?,
	val moment: Instant,
	val reporterUserGlobalId: UUID?,
	val reporterComment: String?,
	val calcedRank: Float,
	val detectedDiagnostics: PayamakReportDiagnostics,
	val expectedDiagnostics: PayamakReportDiagnostics?,
	val processorsReports: Array<PayamakRuleProcessResultReportPublicInput>,
) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as PayamakReportCoreItemInput

		if (calcedRank != other.calcedRank) return false
		if (origin != other.origin) return false
		if (body != other.body) return false
		if (hash != other.hash) return false
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
		result = 31 * result + hash.hashCode()
		result = 31 * result + moment.hashCode()
		result = 31 * result + reporterUserGlobalId.hashCode()
		result = 31 * result + reporterComment.hashCode()
		result = 31 * result + detectedDiagnostics.hashCode()
		result = 31 * result + expectedDiagnostics.hashCode()
		result = 31 * result + processorsReports.contentHashCode()
		return result
	}

	companion object {
		fun core(item: PayamakReportPublicItemInput): PayamakReportCoreItemInput = PayamakReportCoreItemInput(
			item.origin,
			item.body,
			item.hash,
			item.moment,
			item.reporterUserGlobalId,
			item.reporterComment,
			item.calcedRank,
			item.detectedDiagnostics,
			item.expectedDiagnostics,
			item.processorsReports,
		)
	}
}