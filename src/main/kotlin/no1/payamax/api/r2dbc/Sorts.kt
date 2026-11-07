package no1.payamax.api.r2dbc

import no1.payamax.api.exceptions.InvalidValueException
import org.springframework.data.domain.Pageable
import org.springframework.data.relational.core.mapping.RelationalMappingContext
import org.springframework.stereotype.Component

@Component
class Sorts(private val mappingContext: RelationalMappingContext) {
	fun require(pageable: Pageable, type: Class<*>) {
		if (pageable.sort.isUnsorted) {
			return
		}
		val known = knownNames(type)
		val unknown = pageable.sort.firstOrNull { it.property !in known }
		if (unknown != null) {
			throw InvalidValueException("Invalid sort property '${unknown.property}'.")
		}
	}

	private fun knownNames(type: Class<*>): Set<String> {
		val entity = mappingContext.getRequiredPersistentEntity(type)
		return buildSet {
			for (property in entity) {
				add(property.name)
				add(property.columnName.reference)
			}
		}
	}
}
