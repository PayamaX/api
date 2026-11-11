package no1.payamax.api.modules.rankedorigin

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.*
import java.util.*

@RequestMapping("/v1/protected/ranked-origins")
@Secured(value = ["ROLE_ADMIN"])
@RestController
class RankedOriginsProtectedV1Controller(private val protectedProvider: RankedOriginProtectedContract) {
	@GetMapping("/{globalId}")
	suspend fun get(@PathVariable("globalId") globalId: UUID): RankedOriginProtectedPortable = protectedProvider.get(globalId)

	@GetMapping
	suspend fun get(pageable: Pageable): Page<RankedOriginProtectedPortable> = protectedProvider.paged(pageable)

	@PostMapping
	suspend fun post(@RequestBody input: RankedOriginProtectedInput): RankedOriginProtectedPortable = protectedProvider.persist(input)

	@DeleteMapping("/{globalId}")
	suspend fun delete(@PathVariable("globalId") globalId: UUID): RankedOriginProtectedPortable = protectedProvider.delete(globalId)

	@DeleteMapping
	suspend fun delete(globalIds: Array<UUID>) = protectedProvider.delete(globalIds)
}