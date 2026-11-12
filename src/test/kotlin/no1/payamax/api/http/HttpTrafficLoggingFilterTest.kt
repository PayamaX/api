package no1.payamax.api.http

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import no1.payamax.api.config.cors.HttpLogConfiguration
import no1.payamax.api.config.cors.HttpLogProperties
import no1.payamax.api.config.cors.HttpTrafficLoggingFilter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockPart
import java.nio.charset.StandardCharsets.UTF_8

class HttpTrafficLoggingFilterTest {
	private val contextRunner = ApplicationContextRunner()
		.withUserConfiguration(HttpLogConfiguration::class.java)

	@Test
	fun `logging is disabled unless http-log enabled is true`() {
		// Arrange
		val disabled = contextRunner.withPropertyValues("http-log.enabled=false")
		val omitted = contextRunner
		val enabled = contextRunner.withPropertyValues("http-log.enabled=true")
		var disabledPresent = true
		var omittedPresent = true
		var enabledPresent = false

		// Act
		disabled.run { context -> disabledPresent = context.containsBean("httpTrafficLoggingFilter") }
		omitted.run { context -> omittedPresent = context.containsBean("httpTrafficLoggingFilter") }
		enabled.run { context -> enabledPresent = context.containsBean("httpTrafficLoggingFilter") }

		// Assert
		assertFalse(HttpLogProperties().enabled)
		assertFalse(disabledPresent)
		assertFalse(omittedPresent)
		assertTrue(enabledPresent)
	}

	@Test
	fun `logs request and response headers and the request body`() {
		// Arrange
		val request = MockHttpServletRequest("POST", "/origins")
		request.queryString = "q=1"
		request.protocol = "HTTP/1.1"
		request.contentType = MediaType.APPLICATION_JSON_VALUE
		request.characterEncoding = UTF_8.name()
		request.addHeader("Accept", "application/json")
		request.addHeader("Accept", "text/plain")
		request.addHeader("X-Request-Id", "req-1")
		request.setContent("""{"name":"Pāx"}""".toByteArray(UTF_8))
		val response = MockHttpServletResponse()
		var body = ""

		// Act
		val message = capture {
			HttpTrafficLoggingFilter().doFilter(request, response) { req, res ->
				val httpRequest = req as HttpServletRequest
				val httpResponse = res as HttpServletResponse
				body = httpRequest.inputStream.readBytes().toString(UTF_8)
				httpResponse.contentType = MediaType.APPLICATION_JSON_VALUE
				httpResponse.characterEncoding = UTF_8.name()
				httpResponse.setHeader("X-Response-Id", "resp-1")
				httpResponse.status = 201
				httpResponse.writer.write("""{"ok":true}""")
			}
		}

		// Assert
		assertEquals("""{"name":"Pāx"}""", body)
		assertEquals("""{"ok":true}""", response.contentAsString)
		assertEquals(201, response.status)
		assertTrue(message.contains("HTTP exchange POST /origins?q=1 HTTP/1.1 -> 201"))
		assertTrue(message.contains("Accept: application/json"))
		assertTrue(message.contains("Accept: text/plain"))
		assertTrue(message.contains("X-Request-Id: req-1"))
		assertTrue(message.contains("request body:\n{\"name\":\"Pāx\"}"))
		assertTrue(message.contains("X-Response-Id: resp-1"))
		assertFalse(message.contains("response body:"))
		assertFalse(message.contains("{\"ok\":true}"))
	}

	@Test
	fun `logs a request body when content length is unknown and the request is wrapped`() {
		// Arrange
		val request = object : MockHttpServletRequest("POST", "/origins") {
			override fun getContentLength(): Int = -1
			override fun getContentLengthLong(): Long = -1
		}
		request.setContent("""{"name":"Pāx"}""".toByteArray(UTF_8))
		val wrapped = HttpServletRequestWrapper(request)
		val response = MockHttpServletResponse()
		var body = ""

		// Act
		val message = capture {
			HttpTrafficLoggingFilter().doFilter(wrapped, response) { req, _ ->
				body = (req as HttpServletRequest).inputStream.readBytes().toString(UTF_8)
			}
		}

		// Assert
		assertEquals("""{"name":"Pāx"}""", body)
		assertTrue(message.contains("request body:\n{\"name\":\"Pāx\"}"))
	}

	@Test
	fun `logs multipart field bodies and file metadata without file bytes`() {
		// Arrange
		val fileBytes = "FILE_BYTES_SHOULD_NOT_APPEAR".toByteArray(UTF_8)
		val request = MockHttpServletRequest("POST", "/upload")
		request.contentType = "multipart/form-data; boundary=----test"
		request.addHeader("Content-Type", request.contentType!!)
		request.addPart(MockPart("title", "field-value".toByteArray(UTF_8)))
		val file = MockPart("document", "notes.txt", fileBytes)
		file.headers.contentType = MediaType.TEXT_PLAIN
		request.addPart(file)

		val response = MockHttpServletResponse()

		// Act
		val message = capture {
			HttpTrafficLoggingFilter().doFilter(request, response) { _, res ->
				(res as HttpServletResponse).status = 202
			}
		}

		// Assert
		assertTrue(message.contains("part name=title"))
		assertTrue(message.contains("part body:\nfield-value"))
		assertTrue(message.contains("part name=document; filename=notes.txt; contentType=text/plain; size=${fileBytes.size}"))
		assertTrue(message.contains("filename=\"notes.txt\"") || message.contains("filename=notes.txt"))
		assertFalse(message.contains("FILE_BYTES_SHOULD_NOT_APPEAR"))
		assertTrue(message.contains("-> 202"))
	}

	@Test
	fun `returns a binary response without logging its bytes`() {
		// Arrange
		val request = MockHttpServletRequest("GET", "/file")
		val response = MockHttpServletResponse()
		val payload = "BINARY_BODY_SHOULD_NOT_APPEAR".toByteArray(UTF_8)

		// Act
		val message = capture {
			HttpTrafficLoggingFilter().doFilter(request, response) { _, res ->
				val httpResponse = res as HttpServletResponse
				httpResponse.contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE
				httpResponse.setHeader("Content-Disposition", "attachment; filename=\"report.bin\"")
				httpResponse.outputStream.write(payload)
			}
		}

		// Assert
		assertEquals(payload.toList(), response.contentAsByteArray.toList())
		assertTrue(message.contains("Content-Disposition: attachment; filename=\"report.bin\""))
		assertFalse(message.contains("response body:"))
		assertFalse(message.contains("BINARY_BODY_SHOULD_NOT_APPEAR"))
	}

	@Test
	fun `logs the request when processing fails and still throws`() {
		// Arrange
		val request = MockHttpServletRequest("POST", "/origins")
		request.contentType = MediaType.APPLICATION_JSON_VALUE
		request.setContent("""{"a":1}""".toByteArray(UTF_8))
		val response = MockHttpServletResponse()
		var failure: IllegalStateException? = null

		// Act
		val message = capture {
			failure = assertThrows(IllegalStateException::class.java) {
				HttpTrafficLoggingFilter().doFilter(request, response) { _, _ ->
					throw IllegalStateException("rejected")
				}
			}
		}

		// Assert
		assertEquals("rejected", failure!!.message)
		assertTrue(message.contains("failed with java.lang.IllegalStateException: rejected"))
		assertTrue(message.contains("request body:\n{\"a\":1}"))
	}

	private fun capture(block: () -> Unit): String {
		val logger = LoggerFactory.getLogger(HttpTrafficLoggingFilter::class.java) as Logger
		val appender = ListAppender<ILoggingEvent>()
		appender.start()
		val previousLevel = logger.level
		logger.level = ch.qos.logback.classic.Level.INFO
		logger.addAppender(appender)
		try {
			block()
			return appender.list.joinToString("\n") { it.formattedMessage }
		} finally {
			logger.detachAppender(appender)
			logger.level = previousLevel
		}
	}
}
