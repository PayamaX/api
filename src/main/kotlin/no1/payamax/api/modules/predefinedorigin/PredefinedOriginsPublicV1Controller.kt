package no1.payamax.api.modules.predefinedorigin

import jakarta.annotation.security.PermitAll
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/v1/public/predefined-origins")
@RestController
class PredefinedOriginsPublicV1Controller(private val publicProvider: PredefinedOriginPublicContract)
{
	@PermitAll
	@GetMapping
	fun origins(pageable: Pageable): Page<PredefinedOriginPublicPortable> = publicProvider.paged(pageable)
}