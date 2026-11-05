package no1.payamax.api.modules.predefinedorigin

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/v1/protected/predefined-origins")
@RestController
class PredefinedOriginsProtectedV1Controller(private val publicProvider: PredefinedOriginPublicContract)
{
	@Secured(value = ["ROLE_ADMIN"])
	@GetMapping
	fun origins(pageable: Pageable): Page<PredefinedOriginPublicPortable> = publicProvider.paged(pageable)
}