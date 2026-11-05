package no1.payamax.api.exceptions

import org.springframework.http.HttpStatus

class InvalidValueException(message: String = "Invalid value") : SuperException(HttpStatus.BAD_REQUEST, message)
