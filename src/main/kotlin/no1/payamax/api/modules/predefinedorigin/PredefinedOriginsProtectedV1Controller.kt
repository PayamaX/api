package no1.payamax.api.modules.predefinedorigin

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.*
import java.util.*

@RequestMapping("/v1/protected/predefined-origins")
@Secured(value = ["ROLE_ADMIN"])
@RestController
class PredefinedOriginsProtectedV1Controller(private val protectedProvider: PredefinedOriginProtectedContract) {
	@GetMapping("/{globalId}")
	suspend fun get(@PathVariable("globalId") globalId: UUID): PredefinedOriginProtectedPortable = protectedProvider.get(globalId)

	@GetMapping
	suspend fun get(pageable: Pageable): Page<PredefinedOriginProtectedPortable> = protectedProvider.paged(pageable)

	@PostMapping
	suspend fun post(@RequestBody input: PredefinedOriginProtectedInput): PredefinedOriginProtectedPortable = protectedProvider.persist(input)

	@DeleteMapping("/{globalId}")
	suspend fun delete(@PathVariable("globalId") globalId: UUID): PredefinedOriginProtectedPortable = protectedProvider.delete(globalId)

	@DeleteMapping
	suspend fun delete(globalIds: Array<UUID>) = protectedProvider.delete(globalIds)
}