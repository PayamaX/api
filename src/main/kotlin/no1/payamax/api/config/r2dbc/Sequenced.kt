package no1.payamax.api.config.r2dbc

import java.util.UUID

interface Sequenced {
	var id: Long?
	var globalId: UUID
}
