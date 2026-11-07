package no1.payamax.api.config.http.oauth

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.security.web.util.matcher.IpAddressMatcher

@ConfigurationProperties(prefix = "swagger.access")
class SwaggerAccessProperties {
	/**
	 * CIDR ranges, or single addresses, that may open Swagger UI and OpenAPI docs
	 * without ROLE_ADMIN. IPv4 and IPv6 are both accepted.
	 */
	var allowedIpRanges: List<String> = emptyList()

	fun allowedRanges(): List<String> = allowedIpRanges.map { it.trim() }.filter { it.isNotEmpty() }

	fun validate() {
		allowedRanges().forEach { range ->
			try {
				IpAddressMatcher(range)
			} catch (ex: IllegalArgumentException) {
				throw IllegalArgumentException(
					"swagger.access.allowed-ip-ranges contains an invalid IP range: $range",
					ex,
				)
			}
		}
	}
}
