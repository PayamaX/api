package no1.payamax.api.exceptions

import org.springframework.http.HttpStatus
import java.util.UUID

class EntityNotFoundException : SuperException {
	constructor(entityClass: Class<*>, globalId: UUID) : this(entityClass, "globalId", globalId)
	constructor(entityClass: Class<*>, id: Long) : this(entityClass, "id", id)
	constructor(entityClass: Class<*>, property: String, value: Any) : super(HttpStatus.BAD_REQUEST, "${entityClass.simpleName} with $property `$value` not found.")
}