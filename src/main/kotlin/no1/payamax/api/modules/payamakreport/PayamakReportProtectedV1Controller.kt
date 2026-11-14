package no1.payamax.api.modules.payamakreport

import io.swagger.v3.oas.annotations.Operation
import jakarta.annotation.security.PermitAll
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/v1/protected/payamak-reports")
@RestController
@PreAuthorize("hasRole('ADMIN')")
class PayamakReportProtectedV1Controller(private val payamakReportProtectedProvider: PayamakReportProtectedContract) {
	@Operation(operationId = "getProtectedPayamakReports")
	@GetMapping
	suspend fun get(@ParameterObject pageable: Pageable, @ParameterObject filter: PayamakReportProtectedFilter? = null): PagedModel<PayamakReportProtectedPortable> = payamakReportProtectedProvider.paged(pageable, filter)

	@Operation(operationId = "getProtectedPayamakReport")
	@GetMapping("/{globalId}")
	suspend fun get(@PathVariable("globalId") globalId: UUID): PayamakReportProtectedPortable = payamakReportProtectedProvider.single(globalId)

	@Operation(operationId = "deleteProtectedPayamakReport")
	@DeleteMapping("/{globalId}")
	suspend fun delete(@PathVariable("globalId") globalId: UUID): PayamakReportProtectedPortable = payamakReportProtectedProvider.delete(globalId)

	@Operation(operationId = "deleteProtectedPayamakReports")
	@DeleteMapping
	suspend fun delete(globalIds: Array<UUID>): Unit = payamakReportProtectedProvider.delete(globalIds)

	@Operation(operationId = "putProtectedPayamakReport")
	@PutMapping("/{globalId}")
	suspend fun put(@PathVariable("globalId") globalId: UUID, @RequestBody @Valid input: PayamakReportPutProtectedInput): PayamakReportProtectedPortable = payamakReportProtectedProvider.update(globalId, input)

	@Operation(operationId = "patchProtectedPayamakReportFlag")
	@PatchMapping("/{globalId}/flag")
	suspend fun patchFlagged(@PathVariable("globalId") globalId: UUID, flagged: Boolean): PayamakReportProtectedPortable = payamakReportProtectedProvider.flag(globalId, flagged)

	@Operation(operationId = "patchProtectedPayamakReportPostpone")
	@PatchMapping("/{globalId}/postpone")
	suspend fun patchPostponed(@PathVariable("globalId") globalId: UUID): PayamakReportProtectedPortable = payamakReportProtectedProvider.postpone(globalId)
}
