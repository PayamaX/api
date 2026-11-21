package no1.payamax.api.config.oauth

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@ConfigurationProperties(prefix = "oauth2")
class OAuth2Properties {
	var realm: String = ""
	var clientId: String = ""
	var publicRealmUrl: String = ""
	var privateRealmUrl: String = ""
	var healthUrl: String = ""
	var issuers: List<String> = emptyList()
	var requiresSecureMetadata: Boolean = true
	var audiences: List<String> = emptyList()

	fun validate() {
		require(realm.isNotBlank()) { "oauth2.realm must be set." }
		require(clientId.isNotBlank()) { "oauth2.client-id must be set." }
		require(publicRealmUrl.isNotBlank()) { "oauth2.public-realm-url must be set." }
		require(privateRealmUrl.isNotBlank()) { "oauth2.private-realm-url must be set." }
		require(healthUrl.isNotBlank()) { "oauth2.health-url must be set." }
		require(acceptedIssuers().isNotEmpty()) { "oauth2.issuers or oauth2.public-realm-url must be set." }
		require(acceptedAudiences().isNotEmpty()) { "oauth2.audiences or oauth2.client-id must be set." }
		if (requiresSecureMetadata && !privateRealmUrl.startsWith("https://", ignoreCase = true)) {
			throw IllegalArgumentException(
				"oauth2.private-realm-url must use HTTPS when oauth2.requires-secure-metadata is true.",
			)
		}
	}

	fun acceptedIssuers(): Set<String> {
		val configured = issuers.map { it.normalizedUrl() }.filter { it.isNotEmpty() }
		if (configured.isNotEmpty()) {
			return configured.toSet()
		}
		return setOf(publicRealmUrl.normalizedUrl()).filter { it.isNotEmpty() }.toSet()
	}

	fun acceptedAudiences(): Set<String> {
		return (audiences + clientId).map { it.trim() }.filter { it.isNotEmpty() }.toSet()
	}

	fun metadataUrl(): String {
		return "${privateRealmUrl.normalizedUrl()}/.well-known/openid-configuration"
	}

	fun endSessionUrl(): String {
		val client = URLEncoder.encode(clientId.trim(), StandardCharsets.UTF_8)
		return "${publicRealmUrl.normalizedUrl()}/protocol/openid-connect/logout?client_id=$client"
	}
}

internal fun String.normalizedUrl(): String = trim().trimEnd('/')
