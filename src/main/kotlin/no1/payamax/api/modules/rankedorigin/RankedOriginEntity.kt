package no1.payamax.api.modules.rankedorigin

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id as JpaId
import jakarta.persistence.Table as JpaTable
import jakarta.persistence.UniqueConstraint
import no1.payamax.api.config.r2dbc.Sequenced
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Entity
@JpaTable(uniqueConstraints = [
	UniqueConstraint(name = "uq__ranked_origin__global_id", columnNames = ["global_id"]),
	UniqueConstraint(name = "uq__ranked_origin__origin", columnNames = ["origin"]),
])
@Table
class RankedOriginEntity(
	@JpaId
	@Id
	override var id: Long? = null,
	override var globalId: UUID,
	var updated: Instant,
	var deleted: Instant?,
	var origin: String,
	var rank: Double,
	var ownerGlobalId: UUID?,
	var position: Int,
	@Column(length = 400) var comment: String? = null,
) : Sequenced
