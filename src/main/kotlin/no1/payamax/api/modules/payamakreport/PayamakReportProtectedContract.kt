package no1.payamax.api.modules.payamakreport

import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import java.util.UUID

interface PayamakReportProtectedContract {
	suspend fun paged(pageable: Pageable, filter: PayamakReportProtectedFilter?): PagedModel<PayamakReportProtectedPortable>
	suspend fun single(globalId: UUID): PayamakReportProtectedPortable
	suspend fun delete(globalId: UUID): PayamakReportProtectedPortable
	suspend fun delete(globalIds: Array<UUID>)
	suspend fun update(globalId: UUID, input: PayamakReportPutProtectedInput): PayamakReportProtectedPortable
	suspend fun flag(globalId: UUID, flagged: Boolean): PayamakReportProtectedPortable
	suspend fun postpone(globalId: UUID): PayamakReportProtectedPortable
}