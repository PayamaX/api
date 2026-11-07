package no1.payamax.api.exceptions

import org.springframework.http.HttpStatus
import java.util.UUID

class EntityNotFoundException(private val entityClass: Class<*>, globalId: UUID) : SuperException(HttpStatus.BAD_REQUEST, "Entity not found")