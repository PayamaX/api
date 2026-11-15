package no1.payamax.api.modules.payamakreport.payamakreportruleprocessresult

import io.swagger.v3.oas.annotations.Operation
import jakarta.annotation.security.PermitAll
import jakarta.validation.Valid
import no1.payamax.api.modules.payamakreport.PayamakReportPublicContract
import no1.payamax.api.modules.payamakreport.PayamakReportPublicInput
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/v1/protected/payamak-report/rule-process-results")
@PreAuthorize("hasRole('ADMIN')")
@RestController
class PayamakReportRuleProcessResultProtectedV1Controller(
	private val payamakReportRuleProcessResultProtectedProvider: PayamakReportRuleProcessResultProtectedContract,
) {
	@Operation(operationId = "getPayamakReportRuleProcessResults")
	@PostMapping
	suspend fun get(payamakReportGlobalId: UUID): List<PayamakReportRuleProcessResultProtectedPortable> = payamakReportRuleProcessResultProtectedProvider.getByPayamakReportId(payamakReportGlobalId);
}
