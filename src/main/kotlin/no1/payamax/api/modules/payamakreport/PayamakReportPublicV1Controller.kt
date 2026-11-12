package no1.payamax.api.modules.payamakreport

import jakarta.annotation.security.PermitAll
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/v1/public/payamak-report")
@RestController
class PayamakReportPublicV1Controller(private val payamakReportPublicProvider: PayamakReportPublicContract)
{
	@PermitAll
	@PostMapping
	suspend fun post(
		@RequestBody @Valid input: PayamakReportPublicInput,
	): Unit = payamakReportPublicProvider.persist(input);
}
