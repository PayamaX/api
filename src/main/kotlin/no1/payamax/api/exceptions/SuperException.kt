package no1.payamax.api.exceptions

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException

abstract class SuperException(
	status: HttpStatus,
	detail: String,
	cause: Throwable? = null,
) : ErrorResponseException(status, ProblemDetail.forStatusAndDetail(status, detail), cause)
