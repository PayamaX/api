package no1.payamax.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

@SpringBootApplication
@EnableMethodSecurity(jsr250Enabled = true, securedEnabled = true)
class PayamaXApp

fun main(args: Array<String>) {
	runApplication<PayamaXApp>(*args)
}