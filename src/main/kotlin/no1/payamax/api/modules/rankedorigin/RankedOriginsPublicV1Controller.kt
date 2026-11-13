package no1.payamax.api.modules.rankedorigin

import io.swagger.v3.oas.annotations.Operation
import jakarta.annotation.security.PermitAll
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RequestMapping("/v1/public/ranked-origins")
@RestController
class RankedOriginsPublicV1Controller(private val publicProvider: RankedOriginPublicContract)
{
	@Operation(operationId = "getPublicRankedOriginChanges")
	@PermitAll
	@GetMapping("/changes")
	suspend fun get(
		@RequestParam lastSynced: Instant,
		@RequestParam cursorInstant: Instant? = null,
		@RequestParam cursorIdentifier: Long? = null,
	): List<RankedOriginPublicPortable> = publicProvider.changes(lastSynced, RankedOriginCursor.create(cursorInstant, cursorIdentifier))
}
