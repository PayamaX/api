package no1.payamax.api.modules.payamakreport.payamakruleprocessresultreport

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Transient
import jakarta.persistence.UniqueConstraint
import no1.payamax.api.config.r2dbc.Sequenced
import no1.payamax.api.modules.payamakreport.PayamakReportEntity
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID
import jakarta.persistence.Table as JpaTable

data class PayamakRuleProcessResultReportProtectedPortable(
	val id: Long?,
	val globalId: UUID,
	val reportId: Long?,
	val processorName: String,
	val processorVersion: String,
	val generalRank: Float,
	val userOverrideRank: Float?,
	val processorProvidedComment: String,
	val position: Int,
) {
	companion object {
		fun create(entity: PayamakRuleProcessResultReportEntity): PayamakRuleProcessResultReportProtectedPortable = PayamakRuleProcessResultReportProtectedPortable(
			entity.id,
			entity.globalId,
			entity.reportId,
			entity.processorName,
			entity.processorVersion,
			entity.generalRank,
			entity.userOverrideRank,
			entity.processorProvidedComment,
			entity.position,
		)
	}
}