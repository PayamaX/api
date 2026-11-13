package no1.payamax.api.modules.payamakreport

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Id as JpaId
import jakarta.persistence.Table as JpaTable
import jakarta.persistence.UniqueConstraint
import no1.payamax.api.config.r2dbc.Sequenced
import org.springframework.data.annotation.Id as SpgId
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Entity
@JpaTable(
	uniqueConstraints = [UniqueConstraint(name = "uq__payamak_report__global_id", columnNames = ["global_id"])],
	indexes = [
		Index("ix__payamak_report__state", columnList = "state"),
		Index("ix__payamak_report__origin", columnList = "origin"),
		Index("ix__payamak_report__flagged", columnList = "flagged"),
	],
)
@Table
class PayamakReportEntity(
	@JpaId
	@SpgId
	override var id: Long? = null,
	override var globalId: UUID,
	@Enumerated(EnumType.STRING) var state: PayamakReportStates,
	var origin: String,
	var body: String?,
	var sha512: String?,
	var reporterUserGlobalId: UUID?,
	var reporterComment: String?,
	var reviewerComment: String?,
	var flagged: Boolean,
	@Enumerated(EnumType.STRING) var detectedDiagnostics: PayamakReportDiagnostics,
	@Enumerated(EnumType.STRING) var expectedDiagnostics: PayamakReportDiagnostics?,
	@Enumerated(EnumType.STRING) var decidedDiagnostics: PayamakReportDiagnostics?,
) : Sequenced
