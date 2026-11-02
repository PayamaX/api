package no1.payamax.api.modules.predefinedorigin

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "predefined_origin")
class PredefinedOriginEntity(
    @Id
    @Column(name = "global_id", nullable = false, updatable = false)
    var globalId: UUID,

    @Column(name = "origin", nullable = false, length = 255)
    var origin: String,
)
