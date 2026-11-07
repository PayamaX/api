package no1.payamax.api.oauth

import no1.payamax.api.config.http.oauth.SwaggerAccessFilter
import no1.payamax.api.config.http.oauth.SwaggerAccessProperties
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

class SwaggerAccessFilterTest {
	private val filter = accessFilter("127.0.0.1", "::1", "172.16.0.0/12", "10.8.0.0/16")

	@AfterEach
	fun clearSecurity() {
		SecurityContextHolder.clearContext()
	}

	@Test
	fun `allows documentation from an address inside a configured range`() {
		// Arrange
		val requests = listOf(
			request("/swagger-ui/index.html", "172.18.0.4"),
			request("/v3/api-docs", "10.8.1.9"),
			request("/v3/api-docs/public", "127.0.0.1"),
			request("/swagger-ui.html", "0:0:0:0:0:0:0:1"),
		)

		requests.forEach { current ->
			// Act
			val continued = passes(filter, current)

			// Assert
			assertTrue(continued)
		}
	}

	@Test
	fun `allows an IPv4 address carried as an IPv4-mapped IPv6 address`() {
		// Arrange
		val current = request("/v3/api-docs.yaml", "::ffff:10.8.0.4")

		// Act
		val continued = passes(filter, current)

		// Assert
		assertTrue(continued)
	}

	@Test
	fun `allows documentation from anywhere when the caller has ROLE_ADMIN`() {
		// Arrange
		authenticate("ROLE_ADMIN")
		val requests = listOf(
			request("/swagger-ui/swagger-initializer.js", "8.8.8.8"),
			request("/v3/api-docs/protected", "203.0.113.10"),
		)

		requests.forEach { current ->
			// Act
			val continued = passes(filter, current)

			// Assert
			assertTrue(continued)
		}
	}

	@Test
	fun `rejects documentation from outside the ranges without ROLE_ADMIN`() {
		// Arrange
		val requests = listOf(
			request("/swagger-ui/index.html", "8.8.8.8"),
			request("/v3/api-docs.yaml", "11.0.0.1"),
			request("/v3/api-docs.yaml/public", "172.32.0.1"),
			request("/swagger-ui/oauth2-redirect.html", "192.168.1.20"),
		)

		requests.forEach { current ->
			// Act
			val outcome = blocks(filter, current)

			// Assert
			assertFalse(outcome.continued)
			assertTrue(outcome.error is AccessDeniedException)
		}
	}

	@Test
	fun `rejects a caller that has some other role`() {
		// Arrange
		authenticate("ROLE_USER")
		val current = request("/v3/api-docs", "8.8.8.8")

		// Act
		val outcome = blocks(filter, current)

		// Assert
		assertFalse(outcome.continued)
		assertTrue(outcome.error is AccessDeniedException)
	}

	@Test
	fun `leaves every other request alone`() {
		// Arrange
		val current = request("/v1/protected/predefined-origins", "8.8.8.8")

		// Act
		val continued = passes(filter, current)

		// Assert
		assertTrue(continued)
	}

	@Test
	fun `uses the path inside the context path`() {
		// Arrange
		val documentation = request("/swagger-ui/index.html", "8.8.8.8", contextPath = "/payamax")
		val api = request("/v1/protected/predefined-origins", "8.8.8.8", contextPath = "/payamax")

		// Act
		val documentationOutcome = blocks(filter, documentation)
		val apiContinued = passes(filter, api)

		// Assert
		assertFalse(documentationOutcome.continued)
		assertTrue(documentationOutcome.error is AccessDeniedException)
		assertTrue(apiContinued)
	}

	@Test
	fun `with no ranges only ROLE_ADMIN may open the docs`() {
		// Arrange
		val closed = accessFilter()
		val anonymous = request("/v3/api-docs", "127.0.0.1")

		// Act
		val denied = blocks(closed, anonymous)

		// Assert
		assertFalse(denied.continued)
		assertTrue(denied.error is AccessDeniedException)

		// Arrange
		authenticate("ROLE_ADMIN")
		val admin = request("/v3/api-docs", "8.8.8.8")

		// Act
		val continued = passes(closed, admin)

		// Assert
		assertTrue(continued)
	}

	@Test
	fun `ignores blank ranges`() {
		// Arrange
		val properties = SwaggerAccessProperties().apply {
			allowedIpRanges = listOf(" 127.0.0.1 ", "", "  ")
		}

		// Act
		val ranges = properties.allowedRanges()

		// Assert
		assertEquals(listOf("127.0.0.1"), ranges)
		properties.validate()
	}

	@Test
	fun `rejects a range that is not an IP address`() {
		// Arrange
		val properties = SwaggerAccessProperties().apply { allowedIpRanges = listOf("localhost") }

		// Act
		val error = assertThrows(IllegalArgumentException::class.java) { properties.validate() }

		// Assert
		assertTrue(error.message!!.contains("localhost"))
	}

	private fun passes(filter: SwaggerAccessFilter, request: MockHttpServletRequest): Boolean {
		var continued = false
		filter.doFilter(request, MockHttpServletResponse()) { _, _ -> continued = true }
		return continued
	}

	private fun blocks(filter: SwaggerAccessFilter, request: MockHttpServletRequest): Blocked {
		var continued = false
		val error = assertThrows(AccessDeniedException::class.java) {
			filter.doFilter(request, MockHttpServletResponse()) { _, _ -> continued = true }
		}
		return Blocked(continued, error)
	}

	private fun request(path: String, address: String, contextPath: String = ""): MockHttpServletRequest {
		val request = MockHttpServletRequest("GET", contextPath + path)
		request.contextPath = contextPath
		request.requestURI = contextPath + path
		request.remoteAddr = address
		return request
	}

	private fun authenticate(vararg authorities: String) {
		SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
			"user",
			"n/a",
			authorities.map { SimpleGrantedAuthority(it) },
		)
	}

	private fun accessFilter(vararg ranges: String): SwaggerAccessFilter {
		return SwaggerAccessFilter(
			SwaggerAccessProperties().apply { allowedIpRanges = ranges.toList() },
		)
	}

	private data class Blocked(val continued: Boolean, val error: AccessDeniedException)
}
