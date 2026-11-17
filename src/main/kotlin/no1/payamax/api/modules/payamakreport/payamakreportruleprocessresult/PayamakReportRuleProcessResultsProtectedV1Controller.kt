package no1.payamax.api.modules.payamakreport.payamakreportruleprocessresult

import io.swagger.v3.oas.annotations.Operation
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/v1/protected/payamak-report/rule-process-results")
@PreAuthorize("hasRole('ADMIN')")
@RestController
class PayamakReportRuleProcessResultsProtectedV1Controller(
	private val payamakReportRuleProcessResultProtectedProvider: PayamakReportRuleProcessResultProtectedContract,
) {
	@Operation(operationId = "getPayamakReportRuleProcessResults")
	@GetMapping
	suspend fun get(payamakReportGlobalId: UUID): List<PayamakReportRuleProcessResultProtectedPortable> = payamakReportRuleProcessResultProtectedProvider.getByPayamakReportGlobalId(payamakReportGlobalId);
}
