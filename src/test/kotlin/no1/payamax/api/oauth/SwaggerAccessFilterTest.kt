package no1.payamax.api.oauth

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
		assertContinues(filter, request("/swagger-ui/index.html", "172.18.0.4"))
		assertContinues(filter, request("/v3/api-docs", "10.8.1.9"))
		assertContinues(filter, request("/v3/api-docs/public", "127.0.0.1"))
		assertContinues(filter, request("/swagger-ui.html", "0:0:0:0:0:0:0:1"))
	}

	@Test
	fun `allows an IPv4 address carried as an IPv4-mapped IPv6 address`() {
		assertContinues(filter, request("/v3/api-docs.yaml", "::ffff:10.8.0.4"))
	}

	@Test
	fun `allows documentation from anywhere when the caller has ROLE_ADMIN`() {
		authenticate("ROLE_ADMIN")

		assertContinues(filter, request("/swagger-ui/swagger-initializer.js", "8.8.8.8"))
		assertContinues(filter, request("/v3/api-docs/protected", "203.0.113.10"))
	}

	@Test
	fun `rejects documentation from outside the ranges without ROLE_ADMIN`() {
		assertRejected(filter, request("/swagger-ui/index.html", "8.8.8.8"))
		assertRejected(filter, request("/v3/api-docs.yaml", "11.0.0.1"))
		assertRejected(filter, request("/v3/api-docs.yaml/public", "172.32.0.1"))
		assertRejected(filter, request("/swagger-ui/oauth2-redirect.html", "192.168.1.20"))
	}

	@Test
	fun `rejects a caller that has some other role`() {
		authenticate("ROLE_USER")

		assertRejected(filter, request("/v3/api-docs", "8.8.8.8"))
	}

	@Test
	fun `leaves every other request alone`() {
		assertContinues(filter, request("/v1/protected/predefined-origins", "8.8.8.8"))
	}

	@Test
	fun `uses the path inside the context path`() {
		assertRejected(filter, request("/swagger-ui/index.html", "8.8.8.8", contextPath = "/payamax"))
		assertContinues(filter, request("/v1/protected/predefined-origins", "8.8.8.8", contextPath = "/payamax"))
	}

	@Test
	fun `with no ranges only ROLE_ADMIN may open the docs`() {
		val closed = accessFilter()

		assertRejected(closed, request("/v3/api-docs", "127.0.0.1"))
		authenticate("ROLE_ADMIN")
		assertContinues(closed, request("/v3/api-docs", "8.8.8.8"))
	}

	@Test
	fun `ignores blank ranges`() {
		val properties = SwaggerAccessProperties().apply {
			allowedIpRanges = listOf(" 127.0.0.1 ", "", "  ")
		}

		assertEquals(listOf("127.0.0.1"), properties.allowedRanges())
		properties.validate()
	}

	@Test
	fun `rejects a range that is not an IP address`() {
		val properties = SwaggerAccessProperties().apply { allowedIpRanges = listOf("localhost") }

		val error = assertThrows(IllegalArgumentException::class.java) { properties.validate() }

		assertTrue(error.message!!.contains("localhost"))
	}

	private fun assertContinues(filter: SwaggerAccessFilter, request: MockHttpServletRequest) {
		var continued = false
		filter.doFilter(request, MockHttpServletResponse()) { _, _ -> continued = true }
		assertTrue(continued)
	}

	private fun assertRejected(filter: SwaggerAccessFilter, request: MockHttpServletRequest) {
		var continued = false
		assertThrows(AccessDeniedException::class.java) {
			filter.doFilter(request, MockHttpServletResponse()) { _, _ -> continued = true }
		}
		assertFalse(continued)
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
}
