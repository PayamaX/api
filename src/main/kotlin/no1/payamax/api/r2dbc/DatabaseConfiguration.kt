package no1.payamax.api.r2dbc

import org.reactivestreams.Publisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.data.relational.core.mapping.NamingStrategy
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback
import org.springframework.data.relational.core.sql.SqlIdentifier
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Mono

@Configuration
class DatabaseConfiguration {
	@Bean
	fun namingStrategy(): NamingStrategy = object : NamingStrategy {
		override fun getTableName(type: Class<*>): String = super.getTableName(type).removeSuffix("_entity")
	}
}

/*@Component
class SequenceIdCallback(private val databaseClient: DatabaseClient) : BeforeConvertCallback<Sequenced> {
	override fun onBeforeConvert(aggregate: Sequenced, table: SqlIdentifier): Publisher<Sequenced> {
		if (aggregate.id != null) {
			return Mono.just(aggregate)
		}
		return databaseClient.sql("select nextval('id')")
			.map { row -> (row.get(0) as Number).toLong() }
			.one()
			.map { id ->
				aggregate.id = id
				aggregate
			}
	}
}*/
