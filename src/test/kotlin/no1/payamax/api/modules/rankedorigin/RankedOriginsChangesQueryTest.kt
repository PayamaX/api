package no1.payamax.api.modules.rankedorigin

import no1.payamax.api.config.http.json.KotlinInstantConverter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.format.support.DefaultFormattingConversionService
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.UUID
import kotlin.time.Instant

class RankedOriginsChangesQueryTest {
	@Test
	fun `query instants use the millis nanos form`() {
		val provider = RecordingPublicContract()
		val conversion = DefaultFormattingConversionService()
		conversion.addConverter(KotlinInstantConverter())
		val mockMvc = MockMvcBuilders.standaloneSetup(RankedOriginsPublicV1Controller(provider))
			.setConversionService(conversion)
			.build()

		mockMvc.perform(
			get("/v1/public/ranked-origins/changes")
				.param("lastSynced", "2234.567890")
				.param("updated", "2234.567890")
				.param("identifier", "0"),
		).andExpect(status().isOk)

		val instant = Instant.fromEpochSeconds(2, 234_567_890)
		assertEquals(instant, provider.lastSynced)
		assertEquals(RankedOriginCursor(instant, 0), provider.cursor)
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
