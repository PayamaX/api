package no1.payamax.api.config.oauth

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.Scopes
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.ParameterizedTypeReference
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.ExceptionTranslationFilter
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.time.Duration

@Configuration
@EnableConfigurationProperties(OAuth2Properties::class, SwaggerAccessProperties::class)
class OAuth2SecurityConfig(
	private val oauth2: OAuth2Properties,
	private val swaggerAccess: SwaggerAccessProperties,
) {
	init {
		oauth2.validate()
		swaggerAccess.validate()
	}

	@Bean
	fun securityFilterChain(http: HttpSecurity, jwtDecoder: JwtDecoder): SecurityFilterChain {
		http
			// CorsFilter answers browser preflight before authentication.
			.cors { }
			.csrf { it.disable() }
			.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
			.authorizeHttpRequests { requests ->
				requests
					.requestMatchers("/error").permitAll()
					// SwaggerAccessFilter is the gate. These stay unauthenticated so a
					// client inside an allowed range can open the docs without a token.
					.requestMatchers(*swaggerDocumentationPatterns.toTypedArray()).permitAll()
					.anyRequest().permitAll()
			}
			.oauth2ResourceServer { resourceServer ->
				resourceServer.jwt { jwt ->
					jwt.decoder(jwtDecoder)
					jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
				}
			}
			.addFilterAfter(SwaggerAccessFilter(swaggerAccess), ExceptionTranslationFilter::class.java)
		return http.build()
	}

	@Bean
	fun jwtDecoder(): JwtDecoder {
		return LazyJwtDecoder {
			val jwkSetUri = jwkSetUri()
			val decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
			decoder.setJwtValidator(
				DelegatingOAuth2TokenValidator(
					JwtTimestampValidator(Duration.ofMinutes(5)),
					IssuerValidator(oauth2.acceptedIssuers()),
					AudienceValidator(oauth2.acceptedAudiences()),
				),
			)
			decoder
		}
	}

	@Bean
	fun openApi(): OpenAPI {
		val realmUrl = oauth2.publicRealmUrl.normalizedUrl()
		val scheme = "oauth2"
		val flow = OAuthFlow()
			.authorizationUrl("$realmUrl/protocol/openid-connect/auth")
			.tokenUrl("$realmUrl/protocol/openid-connect/token")
			.scopes(
				Scopes()
					.addString("openid", "OpenID Connect")
					.addString("profile", "User Profile")
					.addString("email", "Email Address"),
			)
		return OpenAPI()
			.components(
				Components().addSecuritySchemes(
					scheme,
					SecurityScheme()
						.type(SecurityScheme.Type.OAUTH2)
						.flows(OAuthFlows().authorizationCode(flow)),
				),
			)
			.addSecurityItem(SecurityRequirement().addList(scheme, listOf("openid", "profile", "email")))
	}

	private fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
		val scopes = JwtGrantedAuthoritiesConverter()
		val converter = JwtAuthenticationConverter()
		converter.setPrincipalClaimName("preferred_username")
		converter.setJwtGrantedAuthoritiesConverter { jwt ->
			val authorities = mutableListOf<GrantedAuthority>()
			authorities += scopes.convert(jwt)
			keycloakRoles(jwt.claims, oauth2.clientId, oauth2.acceptedAudiences()).forEach { role ->
				val authority = if (role.startsWith("ROLE_")) role else "ROLE_$role"
				authorities += SimpleGrantedAuthority(authority)
			}
			authorities
		}
		return converter
	}

	private fun jwkSetUri(): String {
		val metadataUrl = oauth2.metadataUrl()
		val document = try {
			RestClient.create()
				.get()
				.uri(metadataUrl)
				.retrieve()
				.body(object : ParameterizedTypeReference<Map<String, Any>>() {})
		} catch (ex: RestClientException) {
			throw IllegalStateException("Could not read OpenID configuration from $metadataUrl.", ex)
		} ?: throw IllegalStateException("OpenID configuration from $metadataUrl was empty.")

		val jwkSetUri = document["jwks_uri"]?.toString()?.normalizedUrl()
			?: throw IllegalStateException("OpenID configuration from $metadataUrl did not include jwks_uri.")
		if (oauth2.requiresSecureMetadata && !jwkSetUri.startsWith("https://", ignoreCase = true)) {
			throw IllegalStateException("JWK set URI must use HTTPS when oauth2.requires-secure-metadata is true.")
		}
		return jwkSetUri
	}
}

internal fun keycloakRoles(claims: Map<String, Any>, clientId: String, audiences: Set<String>): Set<String> {
	val roles = linkedSetOf<String>()
	roles += rolesOf(claims["realm_access"])
	val resourceAccess = claims["resource_access"] as? Map<*, *>
	// azp is the client the user logged in with. Its roles apply when that client
	// is allowed to call this API. oauth2.client-id remains the Swagger UI client.
	val loginClient = claims["azp"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
	val roleClient = if (loginClient != null && loginClient in audiences) loginClient else clientId
	roles += rolesOf(resourceAccess?.get(roleClient))
	return roles
}

private fun rolesOf(access: Any?): List<String> {
	val roles = (access as? Map<*, *>)?.get("roles") as? Collection<*> ?: return emptyList()
	return roles.mapNotNull { it?.toString()?.takeIf(String::isNotBlank) }
}

private class LazyJwtDecoder(
	supplier: () -> JwtDecoder,
) : JwtDecoder {
	private val delegate by lazy(LazyThreadSafetyMode.SYNCHRONIZED, supplier)

	override fun decode(token: String): Jwt = delegate.decode(token)
}

private class IssuerValidator(
	issuers: Set<String>,
) : OAuth2TokenValidator<Jwt> {
	private val issuers = issuers.map { it.normalizedUrl() }.toSet()

	override fun validate(token: Jwt): OAuth2TokenValidatorResult {
		val issuer = token.issuer?.toString()?.normalizedUrl()
		if (issuer != null && issuer in issuers) {
			return OAuth2TokenValidatorResult.success()
		}
		return OAuth2TokenValidatorResult.failure(OAuth2Error("invalid_token", "The iss claim is not trusted", null))
	}
}

private class AudienceValidator(
	private val audiences: Set<String>,
) : OAuth2TokenValidator<Jwt> {
	override fun validate(token: Jwt): OAuth2TokenValidatorResult {
		if (token.audience?.any { it in audiences } == true) {
			return OAuth2TokenValidatorResult.success()
		}
		return OAuth2TokenValidatorResult.failure(OAuth2Error("invalid_token", "The aud claim is not trusted", null))
	}
}
