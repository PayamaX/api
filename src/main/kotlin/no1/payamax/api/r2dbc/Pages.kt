package no1.payamax.api.r2dbc

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

suspend fun <T : Any> Flow<T>.toPage(pageable: Pageable, total: Long): Page<T> =
	PageImpl(toList(), pageable, total)
