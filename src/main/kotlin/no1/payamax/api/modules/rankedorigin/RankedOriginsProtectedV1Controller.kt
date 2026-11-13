package no1.payamax.api.modules.rankedorigin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.security.access.annotation.Secured
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RequestMapping("/v1/protected/ranked-origins")
@Secured(value = ["ROLE_ADMIN"])
//@PreAuthorize("hasRole('ADMIN')")
@RestController
class RankedOriginsProtectedV1Controller(private val protectedProvider: RankedOriginProtectedContract) {

	@Operation(operationId = "getProtectedRankedOrigin")
	@GetMapping("/{identifier}")
	suspend fun get(
		@Parameter(description = "global id or origin") @PathVariable("identifier") identifier: String
	): RankedOriginProtectedPortable = protectedProvider.get(identifier)

	@Operation(operationId = "getProtectedRankedOrigins")
	@GetMapping
	suspend fun get(pageable: Pageable): PagedModel<RankedOriginProtectedPortable> = protectedProvider.paged(pageable)

	@Operation(operationId = "postProtectedRankedOrigin")
	@PostMapping
	suspend fun post(@RequestBody input: RankedOriginProtectedInput): RankedOriginProtectedPortable = protectedProvider.persist(input)

	@Operation(operationId = "deleteProtectedRankedOrigin")
	@DeleteMapping("/{globalId}")
	suspend fun delete(@PathVariable("globalId") globalId: UUID): RankedOriginProtectedPortable = protectedProvider.delete(globalId)

	@Operation(operationId = "deleteProtectedRankedOrigins")
	@DeleteMapping
	suspend fun delete(globalIds: Array<UUID>) = protectedProvider.delete(globalIds)
}