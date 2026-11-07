package no1.payamax.api.config.http

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableConfigurationProperties(CorsProperties::class)
class HttpCorsConfiguration(
	private val cors: CorsProperties,
) {
	init {
		cors.validate()
	}

	/**
	 * The bean name is the one Spring Security looks up. A browser preflight is
	 * answered here, before authentication, so an allowed origin can send
	 * `Authorization` and other request headers.
	 */
	@Bean
	fun corsConfigurationSource(): CorsConfigurationSource = cors.configurationSource()
}

internal fun CorsProperties.configurationSource(): CorsConfigurationSource {
	validate()
	val configuration = CorsConfiguration()
	configuration.allowedOrigins = origins()
	configuration.allowedMethods = methods()
	configuration.allowedHeaders = headers()
	val exposed = exposed()
	if (exposed.isNotEmpty()) {
		configuration.exposedHeaders = exposed
	}
	configuration.maxAge = maxAge!!.seconds
	if (allowCredentials) {
		configuration.allowCredentials = true
	}
	val source = UrlBasedCorsConfigurationSource()
	source.registerCorsConfiguration("/**", configuration)
	return source
}
