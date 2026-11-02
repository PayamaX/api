package no1.payamax.api.rest

import no1.payamax.api.modules.predefinedorigin.PredefinedOriginPublicPortable
import no1.payamax.api.modules.predefinedorigin.PredefinedOriginRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ClientUpdateController(
    private val predefinedOrigins: PredefinedOriginRepository,
) {
    @GetMapping("/origins")
    fun origins(): List<PredefinedOriginPublicPortable> =
        predefinedOrigins.findAll().map { PredefinedOriginPublicPortable(it.globalId, it.origin) }
}