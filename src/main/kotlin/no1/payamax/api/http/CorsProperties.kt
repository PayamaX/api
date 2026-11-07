package no1.payamax.api.http

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI
import java.net.URISyntaxException
import java.time.Duration

@ConfigurationProperties(prefix = "cors")
class CorsProperties {
	/**
	 * Browser origins that may call this API. Each value is a scheme and host,
	 * for example the value of `PAX_ADMIN_URL`. At least one origin is required.
	 * Same-origin callers are unaffected.
	 */
	var allowedOrigins: List<String> = emptyList()

	/**
	 * Methods a browser may use. `*` allows every method. Otherwise each value
	 * is one of GET, HEAD, POST, PUT, PATCH, DELETE, or OPTIONS.
	 */
	var allowedMethods: List<String> = emptyList()

	/**
	 * Request headers a browser may send. `*` allows whichever headers the
	 * preflight asks for, including `Authorization`.
	 */
	var allowedHeaders: List<String> = emptyList()

	/**
	 * Response headers a browser script may read. Leave empty when none are needed.
	 * `*` is rejected while credentials are allowed.
	 */
	var exposedHeaders: List<String> = emptyList()

	/**
	 * How long a browser may cache a preflight response. A whole number of seconds,
	 * for example `3600s`.
	 */
	var maxAge: Duration? = null

	/**
	 * When true, browsers may send cookies on cross-origin calls. Bearer tokens
	 * do not need this.
	 */
	var allowCredentials: Boolean = false

	fun origins(): List<String> = allowedOrigins.map { it.trim().trimEnd('/') }.filter { it.isNotEmpty() }

	fun methods(): List<String> = tokens(allowedMethods, uppercase = true)

	fun headers(): List<String> = tokens(allowedHeaders)

	fun exposed(): List<String> = tokens(exposedHeaders)

	fun validate() {
		val origins = origins()
		if (origins.isEmpty()) {
			throw IllegalArgumentException("cors.allowed-origins must be set.")
		}
		origins.forEach { origin ->
			if (!isOrigin(origin)) {
				throw IllegalArgumentException("cors.allowed-origins contains an invalid origin: $origin")
			}
		}
		validateMethods()
		validateHeaders("cors.allowed-headers", headers(), allowWildcard = true)
		validateHeaders("cors.exposed-headers", exposed(), allowWildcard = !allowCredentials)
		val age = maxAge ?: throw IllegalArgumentException("cors.max-age must be set.")
		if (age.isNegative || age.nano != 0) {
			throw IllegalArgumentException("cors.max-age must be a whole number of seconds.")
		}
	}

	private fun validateMethods() {
		val methods = methods()
		if (methods.isEmpty()) {
			throw IllegalArgumentException("cors.allowed-methods must be set.")
		}
		if ("*" in methods) {
			if (methods.size != 1) {
				throw IllegalArgumentException("cors.allowed-methods cannot mix * with other methods.")
			}
			return
		}
		methods.forEach { method ->
			if (method !in httpMethods) {
				throw IllegalArgumentException("cors.allowed-methods contains an invalid method: $method")
			}
		}
	}

	private fun validateHeaders(name: String, headers: List<String>, allowWildcard: Boolean) {
		if ("*" in headers) {
			if (!allowWildcard) {
				throw IllegalArgumentException("$name cannot be * when cors.allow-credentials is true.")
			}
			if (headers.size != 1) {
				throw IllegalArgumentException("$name cannot mix * with other headers.")
			}
			return
		}
		if (name == "cors.allowed-headers" && headers.isEmpty()) {
			throw IllegalArgumentException("cors.allowed-headers must be set.")
		}
		headers.forEach { header ->
			if (!isHeaderName(header)) {
				throw IllegalArgumentException("$name contains an invalid header: $header")
			}
		}
	}

	private fun isOrigin(origin: String): Boolean {
		if ('*' in origin) {
			return false
		}
		val uri = try {
			URI(origin)
		} catch (ex: URISyntaxException) {
			throw IllegalArgumentException("cors.allowed-origins contains an invalid origin: $origin", ex)
		}
		val scheme = uri.scheme?.lowercase()
		return (scheme == "http" || scheme == "https") &&
			!uri.host.isNullOrBlank() &&
			uri.userInfo == null &&
			uri.rawQuery == null &&
			uri.fragment == null &&
			uri.rawPath.isNullOrEmpty()
	}

	private fun isHeaderName(header: String): Boolean = header.matches(headerName)

	private fun tokens(values: List<String>, uppercase: Boolean = false): List<String> {
		return values.map { value ->
			val trimmed = value.trim()
			if (uppercase) trimmed.uppercase() else trimmed
		}.filter { it.isNotEmpty() }
	}

	private companion object {
		val httpMethods = setOf("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
		val headerName = Regex("[!#${'$'}%&'*+.^_`|~0-9A-Za-z-]+")
	}
}
