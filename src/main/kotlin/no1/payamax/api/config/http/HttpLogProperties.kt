package no1.payamax.api.config.http

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "http-log")
class HttpLogProperties {
	/**
	 * When true, every request and response is written to the log, including headers,
	 * bodies, and multipart file metadata.
	 */
	var enabled: Boolean = false
}
