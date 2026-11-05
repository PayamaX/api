package no1.payamax.api.modules.predefinedorigin

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/v1/protected/predefined-origins")
@Secured(value = ["ROLE_ADMIN"])
@RestController
class PredefinedOriginsProtectedV1Controller(private val publicProvider: PredefinedOriginPublicContract)
{
	@GetMapping
	suspend fun get(pageable: Pageable): Page<PredefinedOriginPublicPortable> = publicProvider.paged(pageable)

	@PostMapping
	suspend fun post(@RequestBody input: PredefinedOriginProtectedInput): PredefinedOriginPublicPortable = publicProvider.persist(input)
}