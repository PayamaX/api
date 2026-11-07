package no1.payamax.api.modules.predefinedorigin

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RequestMapping("/v1/protected/predefined-origins")
@Secured(value = ["ROLE_ADMIN"])
@RestController
class PredefinedOriginsProtectedV1Controller(private val protectedProvider: PredefinedOriginProtectedContract) {
	@GetMapping
	suspend fun get(pageable: Pageable): Page<PredefinedOriginProtectedPortable> = protectedProvider.paged(pageable)

	@PostMapping
	suspend fun post(@RequestBody input: PredefinedOriginProtectedInput): PredefinedOriginProtectedPortable = protectedProvider.persist(input)

	@DeleteMapping("/{globalId}")
	suspend fun delete(@PathVariable("globalId") globalId: UUID) = protectedProvider.delete(globalId)
}