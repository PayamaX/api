package no1.payamax.api.r2dbc

import no1.payamax.api.config.http.r2dbc.Sorts
import no1.payamax.api.exceptions.InvalidValueException
import no1.payamax.api.modules.rankedorigin.RankedOriginEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext
import org.springframework.http.HttpStatus

class SortsTest {
	private val sorts = Sorts(R2dbcMappingContext())

	@Test
	fun `an unknown sort property is a bad request`() {
		// Arrange
		val pageable = PageRequest.of(0, 1, Sort.by("[\"string\"]"))

		// Act
		val ex = assertThrows(InvalidValueException::class.java) {
			sorts.require(pageable, RankedOriginEntity::class.java)
		}

		// Assert
		assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
		assertEquals("Invalid sort property '[\"string\"]'.", ex.body.detail)
	}

	@Test
	fun `a sort name that is not a column is a bad request`() {
		// Arrange
		val pageable = PageRequest.of(0, 1, Sort.by("string"))

		// Act
		val ex = assertThrows(InvalidValueException::class.java) {
			sorts.require(pageable, RankedOriginEntity::class.java)
		}

		// Assert
		assertEquals("Invalid sort property 'string'.", ex.body.detail)
	}

	@Test
	fun `the first unknown property is reported when several are sent`() {
		// Arrange
		val pageable = PageRequest.of(0, 1, Sort.by("origin", "missing"))

		// Act
		val ex = assertThrows(InvalidValueException::class.java) {
			sorts.require(pageable, RankedOriginEntity::class.java)
		}

		// Assert
		assertEquals("Invalid sort property 'missing'.", ex.body.detail)
	}

	@Test
	fun `entity properties and their columns are accepted`() {
		// Arrange
		val pageable = PageRequest.of(
			0,
			1,
			Sort.by(Sort.Order.asc("origin"), Sort.Order.desc("ownerGlobalId"), Sort.Order.asc("owner_global_id")),
		)

		// Act
		val failure = runCatching { sorts.require(pageable, RankedOriginEntity::class.java) }.exceptionOrNull()

		// Assert
		assertNull(failure)
	}

	@Test
	fun `an unsorted page is accepted`() {
		// Arrange
		val pageable = PageRequest.of(0, 1)

		// Act
		val failure = runCatching { sorts.require(pageable, RankedOriginEntity::class.java) }.exceptionOrNull()

		// Assert
		assertNull(failure)
	}
}
