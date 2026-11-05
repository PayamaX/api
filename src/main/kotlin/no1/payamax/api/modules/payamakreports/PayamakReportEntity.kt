package no1.payamax.api.modules.payamakreports

import no1.payamax.api.r2dbc.Sequenced
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table
class PayamakReportEntity(
	@Id
	override var id: Long? = null,
	var globalId: UUID,
	var origin: String,
	var body: String?,
	var hash: String?,
	var reporter: UUID,
	var reporterComment: String?,
	var repeatedCount: Int,
	var desiredDiagnostics: PayamakReportDiagnostics,
	var decidedDiagnostics: PayamakReportDiagnostics?,
) : Sequenced
