package no1.payamax.api.modules.payamakreport.payamakruleprocessresultreport

import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.UniqueConstraint
import no1.payamax.api.config.r2dbc.Sequenced
import no1.payamax.api.modules.payamakreport.PayamakReportEntity
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID
import jakarta.persistence.Table as JpaTable

@JpaTable(uniqueConstraints = [
	UniqueConstraint(name = "uq__payamak_rule_process_result_report__global_id", columnNames = ["global_id"])
])
@Table
@Entity
class PayamakRuleProcessResultReportEntity (
	@jakarta.persistence.Id
	@org.springframework.data.annotation.Id
	override var id: Long?,
	override var globalId: UUID,

	@jakarta.persistence.Transient
	@org.springframework.data.relational.core.mapping.Column("report_id")
	var reportId: Long?,

	@ManyToOne
	@JoinColumn(name = "report_id", referencedColumnName = "id")
	@org.springframework.data.annotation.Transient
	var payamakReport: PayamakReportEntity,
	var processorName: String,
	var processorVersion: String,
	var generalRank: Float,
	var userOverrideRank: Float?,
	var processorProvidedComment: String,
	var position: Int,
): Sequenced