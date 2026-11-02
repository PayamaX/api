package no1.payamax.api.rest

import no1.payamax.api.modules.predefinedorigin.PredefinedOriginPublicPortable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ClientUpdateController(
) {
    @GetMapping("/origins")
    fun origins(): List<PredefinedOriginPublicPortable> = listOf()
}