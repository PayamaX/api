package no1.payamax.api.modules.payamakreport

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id as JpaId
import jakarta.persistence.Table as JpaTable
import jakarta.persistence.UniqueConstraint
import no1.payamax.api.config.r2dbc.Sequenced
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Entity
@JpaTable(uniqueConstraints = [UniqueConstraint(name = "uq__payamak_report___global_id", columnNames = ["global_id"])])
@Table
class PayamakReportEntity(
	@JpaId
	@Id
	override var id: Long? = null,
	override var globalId: UUID,
	var origin: String,
	var body: String?,
	var hash: String?,
	var reporterUserGlobalId: UUID?,
	var reporterComment: String?,
	@Enumerated(EnumType.STRING) var detectedDiagnostics: PayamakReportDiagnostics,
	@Enumerated(EnumType.STRING) var expectedDiagnostics: PayamakReportDiagnostics?,
) : Sequenced
