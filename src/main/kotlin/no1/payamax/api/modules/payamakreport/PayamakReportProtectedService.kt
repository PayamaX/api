package no1.payamax.api.modules.payamakreport

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import no1.payamax.api.config.r2dbc.Sorts
import no1.payamax.api.config.r2dbc.toPage
import no1.payamax.api.exceptions.EntityNotFoundException
import no1.payamax.api.exceptions.InvalidValueException
import no1.payamax.api.modules.payamakreport.payamakreportruleprocessresult.PayamakReportRuleProcessResultRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.core.flow
import org.springframework.data.r2dbc.core.select
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PayamakReportProtectedService(
	private val coreProvider: PayamakReportCoreContract,
	private val repo: PayamakReportRepository,
	private val ruleResultRepo: PayamakReportRuleProcessResultRepository,
	private val entityTemplate: R2dbcEntityTemplate,
	private val sorts: Sorts,
) : PayamakReportProtectedContract {
	override suspend fun paged(
		pageable: Pageable, filter: PayamakReportProtectedFilter?
	): PagedModel<PayamakReportProtectedPortable> {
		sorts.require(pageable, PayamakReportEntity::class.java)
		val query = query(filter)
		val total = entityTemplate.count(query, PayamakReportEntity::class.java).awaitSingle()
		val page = entityTemplate.select<PayamakReportEntity>().matching(query.with(pageable)).flow().toPage(pageable, total).map { PayamakReportProtectedPortable.create(it) }
		return PagedModel(page)
	}

	override suspend fun single(globalId: UUID): PayamakReportProtectedPortable {
		val entity = repo.findByGlobalId(globalId) ?: throw EntityNotFoundException(PayamakReportEntity::class.java, globalId)

		return PayamakReportProtectedPortable.create(entity)
	}

	override suspend fun delete(globalId: UUID): PayamakReportProtectedPortable {
		val entity = repo.findByGlobalId(globalId) ?: throw EntityNotFoundException(PayamakReportEntity::class.java, globalId)
		val processorResults = ruleResultRepo.findAllByPayamakReportId(entity.id!!)
		if(processorResults.isNotEmpty()) {
			ruleResultRepo.deleteAll(processorResults)
		}
		repo.delete(entity)
		return PayamakReportProtectedPortable.create(entity)
	}

	override suspend fun delete(globalIds: Array<UUID>) {
		if (globalIds.isEmpty()) {
			throw InvalidValueException("GlobalIds is empty")
		}
		val entities = repo.findByGlobalIdIn(globalIds)
		if (entities.isEmpty()) {
			throw InvalidValueException("No entity found with ids ${globalIds.contentToString()}")
		}
		val processorResults = ruleResultRepo.findAllByPayamakReportIdIn(entities.map { it.id!! })
		ruleResultRepo.deleteAll(processorResults)
		repo.deleteAll(entities)
	}

	override suspend fun update(
		globalId: UUID, input: PayamakReportPutProtectedInput
	): PayamakReportProtectedPortable {
		val entity = repo.findByGlobalId(globalId) ?: throw EntityNotFoundException(PayamakReportEntity::class.java, globalId)
		entity.reviewerComment = input.reviewerComment
		entity.decidedDiagnostics = input.decidedDiagnostics
		entity.state = PayamakReportStates.REVIEWED
		repo.save(entity)
		return PayamakReportProtectedPortable.create(entity)
	}

	override suspend fun flag(globalId: UUID, flagged: Boolean): PayamakReportProtectedPortable {
		val entity = repo.findByGlobalId(globalId) ?: throw EntityNotFoundException(PayamakReportEntity::class.java, globalId)
		if (entity.flagged == flagged) {
			return PayamakReportProtectedPortable.create(entity)
		}
		entity.flagged = flagged
		repo.save(entity)
		return PayamakReportProtectedPortable.create(entity)
	}

	override suspend fun postpone(globalId: UUID): PayamakReportProtectedPortable {
		val entity = repo.findByGlobalId(globalId) ?: throw EntityNotFoundException(PayamakReportEntity::class.java, globalId)
		if (entity.state == PayamakReportStates.POSTPONED) {
			return PayamakReportProtectedPortable.create(entity)
		}
		entity.state = PayamakReportStates.POSTPONED
		repo.save(entity)
		return PayamakReportProtectedPortable.create(entity)
	}

	private fun query(filter: PayamakReportProtectedFilter?): Query {
		if (filter == null) {
			return Query.empty()
		}
		val criteria = buildList {
			if (filter.state?.isNotEmpty() ?: false) {
				add(Criteria.where("state").`in`(filter.state))
			}
			if (filter.globalId != null) {
				add(Criteria.where("globalId").`is`(filter.globalId))
			}
			if (!filter.origin.isNullOrBlank()) {
				add(Criteria.where("origin").`is`(filter.origin))
			}
			if (filter.onlyFlagged) {
				add(Criteria.where("flagged").`is`(true))
			}
		}
		return Query.query(Criteria.from(criteria))
	}
}