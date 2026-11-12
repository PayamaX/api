package no1.payamax.api.oauth

import no1.payamax.api.config.oauth.injectIdpLogoutPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

class SwaggerUiLogoutTest {
	@Test
	fun `adds an identity provider logout link to the authorize dialog`() {
		// Arrange
		val logoutUrl = "https://auth.payamax.app/realms/pax/protocol/openid-connect/logout?client_id=SwaggerUI"
		val template = initializerTemplate()

		// Act
		val script = injectIdpLogoutPlugin(template, logoutUrl)

		// Assert
		assertTrue(script.contains("SwaggerUIBundle.plugins.DownloadUrl,\n      idpLogoutPlugin"))
		assertTrue(script.contains("function idpLogoutPlugin()"))
		assertTrue(script.contains("wrapComponents:"))
		assertTrue(script.contains("oauth2: function (Original, system)"))
		assertTrue(script.contains("props.authActions.logoutWithPersistOption([props.name])"))
		assertTrue(script.contains("window.location.assign(logoutUrl)"))
		assertTrue(script.contains("\"Logout from identity provider\""))
		assertTrue(script.contains("var logoutUrl = \"$logoutUrl\";"))
		assertEquals(1, script.occurrences("function idpLogoutPlugin()"))
	}

	@Test
	fun `escapes the logout url inside the generated script`() {
		// Arrange
		val template = initializerTemplate()
		val logoutUrl = "https://auth.example/logout?client_id=a\"b<c>"

		// Act
		val script = injectIdpLogoutPlugin(template, logoutUrl)

		// Assert
		assertTrue(script.contains("var logoutUrl = \"https://auth.example/logout?client_id=a\\\"b\\u003cc\\u003e\";"))
	}

	@Test
	fun `rejects an initializer that swagger ui no longer generates`() {
		// Arrange
		val template = "window.ui = SwaggerUIBundle({});"
		val logoutUrl = "https://auth.example/logout"

		// Act
		val error = assertThrows<IllegalArgumentException> {
			injectIdpLogoutPlugin(template, logoutUrl)
		}

		// Assert
		assertTrue(error.message!!.contains("Swagger UI initializer is missing the plugins list."))
	}

	private fun initializerTemplate(): String {
		val resources = PathMatchingResourcePatternResolver().getResources(
			"classpath*:META-INF/resources/webjars/swagger-ui/*/swagger-initializer.js",
		)
		return resources.single().inputStream.bufferedReader().use { it.readText() }
	}

	private fun String.occurrences(needle: String): Int {
		var count = 0
		var from = 0
		while (true) {
			val index = indexOf(needle, from)
			if (index < 0) {
				return count
			}
			count++
			from = index + needle.length
		}
	}
}
