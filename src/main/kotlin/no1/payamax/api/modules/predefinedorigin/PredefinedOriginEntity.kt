package no1.payamax.api.modules.predefinedorigin

import jakarta.persistence.Table as JpaTable
import jakarta.persistence.UniqueConstraint
import no1.payamax.api.config.http.r2dbc.Sequenced
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@JpaTable(uniqueConstraints = [
	UniqueConstraint(name = "uq__predefined_origin__global_id", columnNames = ["global_id"]),
	UniqueConstraint(name = "uq__predefined_origin__origin", columnNames = ["origin"]),
])
@Table
class PredefinedOriginEntity(
	@Id
	override var id: Long? = null,
	override var globalId: UUID,
	var origin: String,
	var rank: Double,
	var ownerGlobalId: UUID?,
	var position: Int = 0,
) : Sequenced
