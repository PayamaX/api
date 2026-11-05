package no1.payamax.api.r2dbc

import java.util.UUID

interface Sequenced {
	var id: Long?
	var globalId: UUID
}
