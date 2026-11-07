package no1.payamax.api.config.http

import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.Part
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.filter.OncePerRequestFilter
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Enumeration

@Configuration
@EnableConfigurationProperties(HttpLogProperties::class)
class HttpLogConfiguration {
	@Bean
	@ConditionalOnProperty(prefix = "http-log", name = ["enabled"], havingValue = "true")
	fun httpTrafficLoggingFilter(): HttpTrafficLoggingFilter = HttpTrafficLoggingFilter()
}

/**
 * Logs the HTTP exchange: request and response headers, the request body, and
 * multipart file metadata. The response body is not logged or buffered, so the
 * application writes it straight to the client. Registered only when `http-log.enabled` is true.
 * Non-multipart request bodies are buffered so they can be logged and still be read by the application.
 * Multipart file contents are not written; the part name, filename, content type, size, and headers are.
 */
class HttpTrafficLoggingFilter : OncePerRequestFilter(), Ordered {
	override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		filterChain: FilterChain,
	) {
		val started = System.nanoTime()
		val parts = captureParts(request)
		// Multipart bodies are logged from parts. Every other body is copied here, before the
		// chain runs, because later filters replace the request and the original stream is then empty.
		val cachedBody = if (parts != null) null else request.inputStream.readBytes()
		val requestToUse = if (cachedBody == null) request else CachedBodyRequestWrapper(request, cachedBody)
		var failure: Exception? = null
		try {
			filterChain.doFilter(requestToUse, response)
		} catch (ex: Exception) {
			failure = ex
		} finally {
			try {
				log.info(
					formatExchange(
						requestToUse,
						response,
						resolveParts(requestToUse, parts),
						cachedBody,
						started,
						failure,
					),
				)
			} catch (ex: Exception) {
				log.warn("Failed to write the HTTP traffic log.", ex)
			}
		}
		if (failure != null) {
			throw failure
		}
	}

	private fun captureParts(request: HttpServletRequest): PartCapture? {
		if (!isMultipart(request)) {
			return null
		}
		return try {
			PartCapture.Ready(request.parts.toList())
		} catch (ex: Exception) {
			PartCapture.Failed(ex)
		}
	}

	private fun resolveParts(request: HttpServletRequest, captured: PartCapture?): PartCapture? {
		if (captured !is PartCapture.Failed) {
			return captured
		}
		return try {
			PartCapture.Ready(request.parts.toList())
		} catch (_: Exception) {
			captured
		}
	}

	private fun formatExchange(
		request: HttpServletRequest,
		response: HttpServletResponse,
		parts: PartCapture?,
		cachedBody: ByteArray?,
		startedNanos: Long,
		failure: Exception?,
	): String {
		val elapsedMs = (System.nanoTime() - startedNanos) / 1_000_000
		return buildString {
			append("HTTP exchange ")
			append(request.method)
			append(' ')
			append(request.requestURI)
			request.queryString?.takeIf { it.isNotEmpty() }?.let {
				append('?')
				append(it)
			}
			append(' ')
			append(request.protocol)
			append(" -> ")
			append(response.status)
			append(" (")
			append(elapsedMs)
			append(" ms)")
			if (failure != null) {
				append(" failed with ")
				append(failure.javaClass.name)
				append(": ")
				append(failure.message.orEmpty())
			}
			append('\n')
			appendHeaders("request headers:", request.headerNames.asList()) { name ->
				request.getHeaders(name).asList()
			}
			appendRequestBody(request, parts, cachedBody)
			appendHeaders("response headers:", response.headerNames) { name ->
				response.getHeaders(name)
			}
		}
	}

	private fun StringBuilder.appendRequestBody(
		request: HttpServletRequest,
		parts: PartCapture?,
		cachedBody: ByteArray?,
	) {
		when (parts) {
			is PartCapture.Failed -> {
				append("request body:\n(multipart unavailable: ")
				append(parts.error.message.orEmpty())
				append(")\n")
			}
			is PartCapture.Ready -> appendParts(request, parts.parts)
			null -> appendPayload(
				"request body:",
				request.contentType,
				request.characterEncoding,
				cachedBody ?: ByteArray(0),
			)
		}
	}

	private fun StringBuilder.appendParts(request: HttpServletRequest, parts: List<Part>) {
		append("request body:\n")
		if (parts.isEmpty()) {
			append("(no parts)\n")
			return
		}
		for (part in parts) {
			val filename = part.submittedFileName?.takeIf { it.isNotBlank() }
			append("part name=")
			append(part.name)
			if (filename != null) {
				append("; filename=")
				append(filename)
			}
			part.contentType?.takeIf { it.isNotBlank() }?.let {
				append("; contentType=")
				append(it)
			}
			append("; size=")
			append(part.size)
			append('\n')
			appendHeaders("part headers:", part.headerNames) { name -> part.getHeaders(name) }
			if (filename == null && isText(part.contentType)) {
				val bytes = try {
					part.inputStream.use { it.readBytes() }
				} catch (ex: Exception) {
					append("part body:\n(unavailable: ")
					append(ex.message.orEmpty())
					append(")\n")
					continue
				}
				appendPayload("part body:", part.contentType, request.characterEncoding, bytes)
			}
		}
	}

	private fun StringBuilder.appendHeaders(
		label: String,
		names: Collection<String>,
		valuesOf: (String) -> Collection<String>,
	) {
		append(label)
		append('\n')
		if (names.isEmpty()) {
			append("(none)\n")
			return
		}
		for (name in names) {
			val values = valuesOf(name)
			if (values.isEmpty()) {
				append(name)
				append(":\n")
			} else {
				for (value in values) {
					append(name)
					append(": ")
					append(value)
					append('\n')
				}
			}
		}
	}

	private fun StringBuilder.appendPayload(
		label: String,
		contentType: String?,
		encoding: String?,
		bytes: ByteArray,
	) {
		append(label)
		append('\n')
		if (bytes.isEmpty()) {
			append("(empty)\n")
			return
		}
		if (!isText(contentType)) {
			append("(binary contentType=")
			append(contentType ?: "unknown")
			append(", size=")
			append(bytes.size)
			append(" bytes)\n")
			return
		}
		val text = String(bytes, charset(contentType, encoding))
		append(text)
		if (!text.endsWith("\n")) {
			append('\n')
		}
	}

	private sealed interface PartCapture {
		data class Ready(val parts: List<Part>) : PartCapture
		data class Failed(val error: Exception) : PartCapture
	}

	private companion object {
		val log = LoggerFactory.getLogger(HttpTrafficLoggingFilter::class.java)

		val textualMediaTypes = setOf(
			"application/json",
			"application/xml",
			"application/javascript",
			"application/x-www-form-urlencoded",
			"application/graphql",
			"application/graphql-response+json",
			"application/problem+json",
			"application/problem+xml",
		)

		fun isMultipart(request: HttpServletRequest): Boolean {
			return request.contentType?.substringBefore(';')?.trim()?.startsWith("multipart/", ignoreCase = true) == true
		}

		fun isText(contentType: String?): Boolean {
			if (contentType.isNullOrBlank()) {
				return true
			}
			val mime = contentType.substringBefore(';').trim().lowercase()
			return mime.startsWith("text/") || mime.endsWith("+json") || mime.endsWith("+xml") || mime in textualMediaTypes
		}

		fun charset(contentType: String?, encoding: String?): Charset {
			val declared = contentType
				?.split(';')
				?.drop(1)
				?.map { it.trim() }
				?.firstOrNull { it.startsWith("charset=", ignoreCase = true) }
				?.substringAfter('=')
				?.trim()
				?.trim('"')
				?.takeIf { it.isNotEmpty() }
			val name = declared ?: encoding?.takeIf { it.isNotBlank() && !it.equals(StandardCharsets.ISO_8859_1.name(), ignoreCase = true) }
			return try {
				if (name.isNullOrBlank()) StandardCharsets.UTF_8 else Charset.forName(name)
			} catch (_: Exception) {
				StandardCharsets.UTF_8
			}
		}

		fun Enumeration<String>?.asList(): List<String> {
			if (this == null) {
				return emptyList()
			}
			return buildList {
				while (hasMoreElements()) {
					add(nextElement())
				}
			}
		}
	}
}

private class CachedBodyRequestWrapper(
	request: HttpServletRequest,
	private val cachedBody: ByteArray,
) : HttpServletRequestWrapper(request) {
	override fun getInputStream(): ServletInputStream = CachedBodyServletInputStream(cachedBody)

	override fun getReader(): BufferedReader {
		val encoding = characterEncoding?.takeIf { it.isNotBlank() } ?: StandardCharsets.UTF_8.name()
		val charset = try {
			Charset.forName(encoding)
		} catch (_: Exception) {
			StandardCharsets.UTF_8
		}
		return BufferedReader(InputStreamReader(ByteArrayInputStream(cachedBody), charset))
	}

	override fun getContentLength(): Int = cachedBody.size

	override fun getContentLengthLong(): Long = cachedBody.size.toLong()
}

private class CachedBodyServletInputStream(
	body: ByteArray,
) : ServletInputStream() {
	private val input = ByteArrayInputStream(body)

	override fun read(): Int = input.read()

	override fun read(b: ByteArray, off: Int, len: Int): Int = input.read(b, off, len)

	override fun isFinished(): Boolean = input.available() == 0

	override fun isReady(): Boolean = true

	override fun setReadListener(readListener: ReadListener) {
		throw UnsupportedOperationException("Non-blocking I/O is not supported for HTTP traffic logging.")
	}
}
