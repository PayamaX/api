package no1.payamax.api.modules.rankedorigin

import kotlinx.coroutines.flow.toList
import no1.payamax.api.exceptions.InvalidValueException
import org.springframework.data.domain.Sort
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.core.flow
import org.springframework.data.r2dbc.core.select
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class RankedOriginCoreService(
	private val repository: RankedOriginRepository,
	private val entityTemplate: R2dbcEntityTemplate
) : RankedOriginCoreContract {
	override suspend fun persist(input: RankedOriginCoreInput): RankedOriginEntity {
		if (input.rank !in 0.0..1.0) {
			throw InvalidValueException("Rank must be between 0 and 1.")
		}
		val now = Instant.now()
		var persisted = repository.findOneByOrigin(input.origin)
		if (persisted != null) {
			if (input.globalId != null && persisted.globalId != input.globalId) {
				throw InvalidValueException()
			}
			persisted.globalId = input.globalId ?: UUID.randomUUID()
			persisted.updated = now
			persisted.deleted = null
			persisted.origin = input.origin
			persisted.rank = input.rank
			persisted.ownerGlobalId = input.ownerGlobalId
			persisted.position = input.position
			persisted.comment = input.comment
		} else {
			persisted = RankedOriginEntity(
				null,
				UUID.randomUUID(),
				now,
				null,
				input.origin,
				input.rank,
				input.ownerGlobalId,
				input.position,
				input.comment,
			)
		}
		return repository.save(persisted)
	}

	override suspend fun changes(lastSynced: Instant, cursor: RankedOriginCursor?): List<RankedOriginEntity> {
		val criteria = if (cursor == null) {
			Criteria.where("updated").greaterThan(lastSynced)
		} else {
			Criteria
				.where("updated").greaterThan(cursor.updated)
				.or(
					Criteria
						.where("updated").`is`(cursor.updated)
						.and("id").greaterThan(cursor.identifier)
				)
		}
		return entityTemplate.select<RankedOriginEntity>()
			.matching(
				Query.query(criteria)
					.sort(Sort.by(Sort.Order.asc("updated"), Sort.Order.asc("id")))
					.limit(1000),
			)
			.flow()
			.toList()
	}
}