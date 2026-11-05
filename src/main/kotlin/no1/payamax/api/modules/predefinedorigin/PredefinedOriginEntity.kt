package no1.payamax.api.modules.predefinedorigin

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

@Entity
@Table
class PredefinedOriginEntity(
	@Id
	@Column(nullable = false, updatable = false)
	var id: Long,

	@Column(nullable = false, updatable = false, unique = true)
	var globalId: UUID,

	@Column(nullable = false, length = 255)
	var origin: String,

	@Column(nullable = false)
	var rank: Double,

	@Column(nullable = true)
	var ownerGlobalId: UUID?,

	@Column
	var position: Int = 0,
)
