package no1.payamax.api.http

import no1.payamax.api.config.http.CorsProperties
import no1.payamax.api.config.http.HttpCorsConfiguration
import no1.payamax.api.config.http.configurationSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import java.time.Duration

class HttpCorsConfigurationTest {
	private val contextRunner = ApplicationContextRunner()
		.withUserConfiguration(HttpCorsConfiguration::class.java)

	@Test
	fun `publishes the source under the name Spring Security looks up`() {
		// Arrange
		val runner = contextRunner.withPropertyValues(*configuredProperties())
		var source: Any? = null
		var present = false

		// Act
		runner.run { context ->
			present = context.containsBean("corsConfigurationSource")
			source = context.getBean("corsConfigurationSource")
		}

		// Assert
		assertTrue(present)
		assertTrue(source is CorsConfigurationSource)
	}

	@Test
	fun `the admin origin is taken from PAX_ADMIN_URL`() {
		// Arrange
		val resource = checkNotNull(javaClass.classLoader.getResource("application.properties"))

		// Act
		val properties = resource.readText()

		// Assert
		assertTrue(properties.contains($$"cors.allowed-origins[0]=${PAX_ADMIN_URL}"))
		assertTrue(properties.contains("cors.allowed-headers[0]=*"))
		assertTrue(properties.contains("cors.max-age=3600s"))
		assertTrue(properties.contains("cors.allow-credentials=false"))
	}

	@Test
	fun `an unresolved admin url fails startup`() {
		// Arrange
		val runner = contextRunner.withPropertyValues(*configuredProperties(origin = $$"${PAX_ADMIN_URL_MISSING}"))
		var failure: Throwable? = null

		// Act
		runner.run { context ->
			failure = context.startupFailure
		}

		// Assert
		assertNotNull(failure)
	}

	@Test
	fun `a blank admin url fails startup`() {
		// Arrange
		val runner = contextRunner.withPropertyValues(*configuredProperties(origin = "   "))
		var failure: Throwable? = null

		// Act
		runner.run { context ->
			failure = context.startupFailure
		}

		// Assert
		assertTrue(failureText(checkNotNull(failure)).contains("cors.allowed-origins must be set."))
	}

	@Test
	fun `rejects a max age that is not a duration`() {
		// Arrange
		val values = configuredProperties().map {
			if (it.startsWith("cors.max-age=")) "cors.max-age=later" else it
		}
		val runner = contextRunner.withPropertyValues(*values.toTypedArray())
		var failure: Throwable? = null

		// Act
		runner.run { context ->
			failure = context.startupFailure
		}

		// Assert
		assertNotNull(failure)
	}

	@Test
	fun `ignores blank origins`() {
		// Arrange
		val properties = configured(" http://localhost:5173/ ", "", "  ")

		// Act
		val origins = properties.origins()

		// Assert
		assertEquals(listOf("http://localhost:5173"), origins)
		properties.validate()
	}

	@Test
	fun `rejects an origin that is not a scheme and host`() {
		// Arrange
		val properties = configured("https://admin.example/app")

		// Act
		val error = assertThrows(IllegalArgumentException::class.java) { properties.validate() }

		// Assert
		assertTrue(error.message!!.contains("https://admin.example/app"))
	}

	@Test
	fun `rejects a wildcard origin`() {
		// Arrange
		val properties = configured("*")

		// Act
		val error = assertThrows(IllegalArgumentException::class.java) { properties.validate() }

		// Assert
		assertTrue(error.message!!.contains("*"))
	}

	@Test
	fun `accepts an address origin`() {
		// Arrange
		val properties = configured("http://[::1]:5173")

		// Act
		val origins = properties.origins()

		// Assert
		assertEquals(listOf("http://[::1]:5173"), origins)
		properties.validate()
	}

	@Test
	fun `rejects a method that is not an http method`() {
		// Arrange
		val properties = configured("http://localhost:5173").apply { allowedMethods = listOf("GET", "FOO") }

		// Act
		val error = assertThrows(IllegalArgumentException::class.java) { properties.validate() }

		// Assert
		assertTrue(error.message!!.contains("FOO"))
	}

	@Test
	fun `rejects a blank header list`() {
		// Arrange
		val properties = configured("http://localhost:5173").apply { allowedHeaders = listOf("  ", "") }

		// Act
		val error = assertThrows(IllegalArgumentException::class.java) { properties.validate() }

		// Assert
		assertTrue(error.message!!.contains("cors.allowed-headers must be set."))
	}

	@Test
	fun `rejects a wildcard exposed header when credentials are allowed`() {
		// Arrange
		val properties = configured("http://localhost:5173").apply {
			allowCredentials = true
			exposedHeaders = listOf("*")
		}

		// Act
		val error = assertThrows(IllegalArgumentException::class.java) { properties.validate() }

		// Assert
		assertTrue(error.message!!.contains("cors.exposed-headers"))
	}

	@Test
	fun `rejects a max age that is not whole seconds`() {
		// Arrange
		val properties = configured("http://localhost:5173").apply { maxAge = Duration.ofMillis(1500) }

		// Act
		val error = assertThrows(IllegalArgumentException::class.java) { properties.validate() }

		// Assert
		assertTrue(error.message!!.contains("cors.max-age"))
	}

	@Test
	fun `preflight from an allowed origin lists the requested headers`() {
		// Arrange
		val filter = CorsFilter(configured("http://localhost:5173").configurationSource())
		val request = crossOrigin("OPTIONS", "http://localhost:5173")
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type, X-FB-CLIENT")
		val response = MockHttpServletResponse()
		var continued = false

		// Act
		filter.doFilter(request, response) { _, _ -> continued = true }

		// Assert
		assertFalse(continued)
		assertEquals(HttpStatus.OK.value(), response.status)
		assertEquals("http://localhost:5173", response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
		assertEquals("3600", response.getHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE))
		val allowedHeaders = response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)!!
		assertTrue(allowedHeaders.contains("Authorization"))
		assertTrue(allowedHeaders.contains("X-FB-CLIENT"))
		assertNull(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
	}

	@Test
	fun `preflight uses the configured age methods and exposed headers`() {
		// Arrange
		val properties = configured("https://admin.example").apply {
			allowedMethods = listOf("get", "options")
			allowedHeaders = listOf("Authorization")
			exposedHeaders = listOf("Location")
			maxAge = Duration.ofMinutes(1)
			allowCredentials = true
		}
		val filter = CorsFilter(properties.configurationSource())
		val request = crossOrigin("OPTIONS", "https://admin.example")
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization")
		val response = MockHttpServletResponse()

		// Act
		filter.doFilter(request, response) { _, _ -> }

		// Assert
		assertEquals("https://admin.example", response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
		assertEquals("60", response.getHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE))
		assertEquals("true", response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
		assertTrue(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)!!.contains("GET"))
		assertEquals("Authorization", response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
	}

	@Test
	fun `a method that was not configured is rejected`() {
		// Arrange
		val properties = configured("http://localhost:5173").apply { allowedMethods = listOf("GET", "OPTIONS") }
		val filter = CorsFilter(properties.configurationSource())
		val request = crossOrigin("OPTIONS", "http://localhost:5173")
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE")
		val response = MockHttpServletResponse()
		var continued = false

		// Act
		filter.doFilter(request, response) { _, _ -> continued = true }

		// Assert
		assertFalse(continued)
		assertEquals(HttpStatus.FORBIDDEN.value(), response.status)
	}

	@Test
	fun `an allowed origin can read the response`() {
		// Arrange
		val properties = configured("https://admin.example").apply { exposedHeaders = listOf("Location") }
		val filter = CorsFilter(properties.configurationSource())
		val request = crossOrigin("GET", "https://admin.example")
		val response = MockHttpServletResponse()
		var continued = false

		// Act
		filter.doFilter(request, response) { _, _ -> continued = true }

		// Assert
		assertTrue(continued)
		assertEquals("https://admin.example", response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
		assertEquals("Location", response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS))
	}

	@Test
	fun `a different origin is rejected before the application runs`() {
		// Arrange
		val filter = CorsFilter(configured("http://localhost:5173").configurationSource())
		val request = crossOrigin("GET", "https://elsewhere.example")
		val response = MockHttpServletResponse()
		var continued = false

		// Act
		filter.doFilter(request, response) { _, _ -> continued = true }

		// Assert
		assertFalse(continued)
		assertEquals(HttpStatus.FORBIDDEN.value(), response.status)
		assertEquals("Invalid CORS request", response.contentAsString)
		assertNull(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
	}

	@Test
	fun `same origin requests are left alone`() {
		// Arrange
		val filter = CorsFilter(configured("http://localhost:5173").configurationSource())
		val request = crossOrigin("POST", "http://localhost:19100", serverPort = 19100)
		val response = MockHttpServletResponse()
		var continued = false

		// Act
		filter.doFilter(request, response) { _, _ -> continued = true }

		// Assert
		assertTrue(continued)
		assertNull(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
	}

	private fun configured(vararg origins: String): CorsProperties {
		return CorsProperties().apply {
			allowedOrigins = origins.toList()
			allowedMethods = listOf("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
			allowedHeaders = listOf("*")
			maxAge = Duration.ofSeconds(3600)
		}
	}

	private fun configuredProperties(origin: String = "http://localhost:5173"): Array<String> {
		return arrayOf(
			"cors.allowed-origins[0]=$origin",
			"cors.allowed-methods[0]=GET",
			"cors.allowed-methods[1]=POST",
			"cors.allowed-methods[2]=OPTIONS",
			"cors.allowed-headers[0]=*",
			"cors.max-age=3600s",
			"cors.allow-credentials=false",
		)
	}

	private fun failureText(failure: Throwable): String {
		val parts = mutableListOf<String>()
		var current: Throwable? = failure
		while (current != null && parts.size < 8) {
			current.message?.let { parts += it }
			current = current.cause
		}
		return parts.joinToString("\n")
	}

	private fun crossOrigin(method: String, origin: String, serverPort: Int = 19100): MockHttpServletRequest {
		val request = MockHttpServletRequest(method, "/v1/protected/ranked-origins")
		request.scheme = "http"
		request.serverName = "localhost"
		request.serverPort = serverPort
		request.addHeader(HttpHeaders.ORIGIN, origin)
		return request
	}
}
