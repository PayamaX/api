package no1.payamax.api.exceptions

import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

class GlobalExceptionHandlerTest {
	private val mockMvc = MockMvcBuilders
		.standaloneSetup(ExceptionHandlerFixtureController())
		.setControllerAdvice(GlobalExceptionHandler())
		.build()

	@Test
	fun `invalid value is a bad request`() {
		// Arrange
		val request = get("/invalid")

		// Act
		val response = mockMvc.perform(request)

		// Assert
		response
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.title").value("Bad Request"))
			.andExpect(jsonPath("$.detail").value("Invalid value"))
	}

	@Test
	fun `invalid value keeps the exception message`() {
		// Arrange
		val request = get("/invalid-message")

		// Act
		val response = mockMvc.perform(request)

		// Assert
		response
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.detail").value("Origin is already registered."))
	}

	@Test
	fun `every super exception becomes a problem detail`() {
		// Arrange
		val request = get("/conflict")

		// Act
		val response = mockMvc.perform(request)

		// Assert
		response
			.andExpect(status().isConflict)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Conflict"))
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.detail").value("Already stored."))
			.andExpect(jsonPath("$.instance").value("/conflict"))
	}

	@Test
	fun `authentication failure is unauthorized`() {
		// Arrange
		val request = get("/unauthenticated")

		// Act
		val response = mockMvc.perform(request)

		// Assert
		response
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.detail").value("Authentication required"))
	}

	@Test
	fun `access denied is forbidden`() {
		// Arrange
		val request = get("/denied")

		// Act
		val response = mockMvc.perform(request)

		// Assert
		response
			.andExpect(status().isForbidden)
			.andExpect(jsonPath("$.detail").value("Access denied"))
	}

	@Test
	fun `unexpected failures hide the cause`() {
		// Arrange
		val request = get("/boom")

		// Act
		val response = mockMvc.perform(request)

		// Assert
		response
			.andExpect(status().isInternalServerError)
			.andExpect(jsonPath("$.status").value(500))
			.andExpect(jsonPath("$.detail").value("Unexpected error"))
			.andExpect(content().string(not(containsString("secret"))))
	}

	@Test
	fun `invalid request content lists each rejected value`() {
		// Arrange
		val request = get("/invalid-content")

		// Act
		val response = mockMvc.perform(request)

		// Assert
		response
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.detail").value("Invalid request content."))
			.andExpect(jsonPath("$.errors[0].field").value("origin"))
			.andExpect(jsonPath("$.errors[0].message").value("must not be blank"))
			.andExpect(jsonPath("$.errors[1].field").value("rank"))
			.andExpect(jsonPath("$.errors[1].message").value("must be greater than 0"))
			.andExpect(jsonPath("$.errors[2].field").value("input"))
			.andExpect(jsonPath("$.errors[2].message").value("origin and rank conflict"))
	}

	@Test
	fun `malformed json stays a bad request`() {
		// Arrange
		val request = post("/echo")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{")

		// Act
		val response = mockMvc.perform(request)

		// Assert
		response
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.status").value(400))
	}
}

@RestController
private class ExceptionHandlerFixtureController {
	@GetMapping("/invalid")
	fun invalid(): Unit = throw InvalidValueException()

	@GetMapping("/invalid-message")
	fun invalidMessage(): Unit = throw InvalidValueException("Origin is already registered.")

	@GetMapping("/conflict")
	fun conflict(): Unit = throw ConflictException("Already stored.")

	@GetMapping("/unauthenticated")
	fun unauthenticated(): Unit = throw object : AuthenticationException("missing token") {}

	@GetMapping("/denied")
	fun denied(): Unit = throw AccessDeniedException("missing ROLE_ADMIN")

	@GetMapping("/boom")
	fun boom(): Unit = throw IllegalStateException("secret database password")

	@GetMapping("/invalid-content")
	fun invalidContent(): Unit = throw invalidRequestContent()

	@PostMapping("/echo")
	fun echo(@RequestBody body: Map<String, String>): Map<String, String> = body
}

private class ConflictException(message: String) : SuperException(HttpStatus.CONFLICT, message)

private fun invalidRequestContent(): MethodArgumentNotValidException {
	val binding = BeanPropertyBindingResult(mapOf("origin" to ""), "input")
	binding.addError(FieldError("input", "origin", "must not be blank"))
	binding.addError(FieldError("input", "rank", "must be greater than 0"))
	binding.addError(ObjectError("input", "origin and rank conflict"))
	val method = ExceptionHandlerFixtureController::class.java.getDeclaredMethod("echo", Map::class.java)
	return MethodArgumentNotValidException(MethodParameter(method, 0), binding)
}
