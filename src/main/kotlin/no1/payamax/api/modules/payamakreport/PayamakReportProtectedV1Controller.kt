package no1.payamax.api.modules.payamakreport

import jakarta.annotation.security.PermitAll
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/v1/protected/payamak-report")
@RestController
@PreAuthorize("hasRole('ADMIN')")
class PayamakReportProtectedV1Controller(private val payamakReportProtectedProvider: PayamakReportProtectedContract) {
	@GetMapping
	suspend fun get(pageable: Pageable, filter: PayamakReportProtectedFilter): Page<PayamakReportPublicPortable> = TODO()
}
