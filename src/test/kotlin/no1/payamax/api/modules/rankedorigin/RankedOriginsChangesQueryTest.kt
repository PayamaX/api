package no1.payamax.api.modules.rankedorigin

import no1.payamax.api.config.http.json.KotlinInstantConverter
import no1.payamax.api.exceptions.GlobalExceptionHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.format.support.DefaultFormattingConversionService
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.UUID
import java.time.Instant

class RankedOriginsChangesQueryTest {
	@Test
	fun `query instants use the seconds nanos form`() {
		val provider = RecordingPublicContract()
		val conversion = DefaultFormattingConversionService()
		conversion.addConverter(KotlinInstantConverter())
		val mockMvc = MockMvcBuilders.standaloneSetup(RankedOriginsPublicV1Controller(provider))
			.setControllerAdvice(GlobalExceptionHandler())
			.setConversionService(conversion)
			.build()

		mockMvc.perform(
			get("/v1/public/ranked-origins/changes")
				.param("lastSynced", "2.234567890")
				.param("cursorInstant", "2.234567890")
				.param("cursorIdentifier", "0"),
		).andExpect(status().isOk)

		val instant = Instant.ofEpochSecond(2, 234_567_890)
		assertEquals(instant, provider.lastSynced)
		assertEquals(RankedOriginCursor(instant, 0), provider.cursor)
	}

	@Test
	fun `a cursor needs both instant and identifier`() {
		val provider = RecordingPublicContract()
		val conversion = DefaultFormattingConversionService()
		conversion.addConverter(KotlinInstantConverter())
		val mockMvc = MockMvcBuilders.standaloneSetup(RankedOriginsPublicV1Controller(provider))
			.setControllerAdvice(GlobalExceptionHandler())
			.setConversionService(conversion)
			.build()

		val actions = mockMvc.perform(
			get("/v1/public/ranked-origins/changes")
				.param("lastSynced", "2.234567890")
				.param("cursorInstant", "2.234567890"),
		)
		val result = actions.andReturn()
		val response = if (result.request.isAsyncStarted) mockMvc.perform(asyncDispatch(result)) else actions
		response
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.title").value("Bad Request"))
			.andExpect(jsonPath("$.detail").value("cursorInstant and cursorIdentifier must be supplied together."))
			.andExpect(jsonPath("$.instance").value("/v1/public/ranked-origins/changes"))
	}

	private class RecordingPublicContract : RankedOriginPublicContract {
		var lastSynced: Instant? = null
		var cursor: RankedOriginCursor? = null

		override suspend fun changes(lastSynced: Instant, cursor: RankedOriginCursor?): List<RankedOriginPublicPortable> {
			this.lastSynced = lastSynced
			this.cursor = cursor
			return emptyList()
		}

		override suspend fun paged(pageable: Pageable, lastSynced: Instant): Page<RankedOriginPublicPortable> =
			Page.empty()

		override suspend fun paged(pageable: Pageable, ownerId: UUID): Page<RankedOriginPublicPortable> =
			Page.empty()
	}
}
