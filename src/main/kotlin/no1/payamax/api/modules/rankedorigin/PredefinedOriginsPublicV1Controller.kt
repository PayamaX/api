package no1.payamax.api.modules.rankedorigin

import jakarta.annotation.security.PermitAll
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/v1/public/ranked-origins")
@RestController
class RankedOriginsPublicV1Controller(private val publicProvider: RankedOriginPublicContract)
{
	@PermitAll
	@GetMapping
	suspend fun get(pageable: Pageable): Page<RankedOriginPublicPortable> = publicProvider.paged(pageable)
}