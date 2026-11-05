package no1.payamax.api.exceptions

import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
	@ExceptionHandler(InvalidValueException::class)
	fun handleInvalidValue(ex: InvalidValueException, request: WebRequest): ResponseEntity<Any>? =
		respond(ex, HttpStatus.BAD_REQUEST, ex.message ?: "Invalid value", request)

	@ExceptionHandler(AuthenticationException::class)
	fun handleUnauthenticated(ex: AuthenticationException, request: WebRequest): ResponseEntity<Any>? =
		respond(ex, HttpStatus.UNAUTHORIZED, "Authentication required", request)

	@ExceptionHandler(AccessDeniedException::class)
	fun handleAccessDenied(ex: AccessDeniedException, request: WebRequest): ResponseEntity<Any>? =
		respond(ex, HttpStatus.FORBIDDEN, "Access denied", request)

	@ExceptionHandler(Exception::class)
	fun handleUnexpected(ex: Exception, request: WebRequest): ResponseEntity<Any>? {
		log.error("Unhandled exception.", ex)
		return respond(ex, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request)
	}

	private fun respond(
		ex: Exception,
		status: HttpStatusCode,
		detail: String,
		request: WebRequest,
	): ResponseEntity<Any>? {
		val body = createProblemDetail(ex, status, detail, null, null, request)
		return handleExceptionInternal(ex, body, HttpHeaders(), status, request)
	}

	private companion object {
		val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
	}
}
