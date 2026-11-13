package no1.payamax.api.exceptions

import org.springframework.http.HttpStatus
import java.util.UUID

class EntityNotFoundException : SuperException {
	constructor(entityClass: Class<*>, globalId: UUID) : super(HttpStatus.BAD_REQUEST, "Entity not found")
	constructor(entityClass: Class<*>, property: String, expectedValue: Any) : super(HttpStatus.BAD_REQUEST, "Entity not found")
}