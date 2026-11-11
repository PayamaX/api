package no1.payamax.api.modules.rankedorigin

import io.swagger.v3.oas.annotations.Parameter
import no1.payamax.api.exceptions.InvalidValueException
import java.time.Instant

data class RankedOriginCursor(
	val updated: Instant,
	val identifier: Long,
) {
	companion object {
		fun create(cursorInstant: Instant?, cursorIdentifier: Long?): RankedOriginCursor? {
			if (cursorInstant == null && cursorIdentifier == null) {
				return null
			}
			if (cursorInstant != null && cursorIdentifier != null) {
				return RankedOriginCursor(updated = cursorInstant, identifier = cursorIdentifier)
			}
			throw InvalidValueException("cursorInstant and cursorIdentifier must be supplied together.")
		}
	}
}
