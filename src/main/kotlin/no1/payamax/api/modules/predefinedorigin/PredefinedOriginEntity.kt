package no1.payamax.api.modules.predefinedorigin

import no1.payamax.api.r2dbc.Sequenced
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table
class PredefinedOriginEntity(
	@Id
	override var id: Long? = null,
	var globalId: UUID,
	var origin: String,
	var rank: Double,
	var ownerGlobalId: UUID?,
	var position: Int = 0,
) : Sequenced
