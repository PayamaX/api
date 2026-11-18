package no1.payamax.api.modules.payamakreport.payamakreportruleprocessresult

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.UniqueConstraint
import no1.payamax.api.config.r2dbc.Sequenced
import no1.payamax.api.modules.payamakreport.PayamakReportEntity
import org.hibernate.annotations.ColumnDefault
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID
import jakarta.persistence.Table as JpaTable

@JpaTable(
	uniqueConstraints = [UniqueConstraint(name = "uq__payamak_report_rule_process_result__global_id", columnNames = ["global_id"])],
	indexes = [Index("ix__payamak_report_rule_process_result__payamak_report_id", columnList = "payamak_report_id")]
)
@Table
@Entity
class PayamakReportRuleProcessResultEntity (
	@jakarta.persistence.Id
	@org.springframework.data.annotation.Id
	@ColumnDefault("nextval('id')")
	override var id: Long?,
	override var globalId: UUID,

	@jakarta.persistence.Transient
	@org.springframework.data.relational.core.mapping.Column("payamak_report_id")
	var payamakReportId: Long?,

	@ManyToOne
	@JoinColumn(name = "payamak_report_id", referencedColumnName = "id")
	@org.springframework.data.annotation.Transient
	var payamakReport: PayamakReportEntity? = null,

	@Column(length = 32)
	var processorName: String,

	var processorVersion: Int,
	var generalRank: Float,
	var userOverrideRank: Float?,

	@Column(length = 4096)
	var processorProvidedComment: String?,
	var position: Int,
): Sequenced