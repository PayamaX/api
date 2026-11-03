package no1.payamax.api.oauth

import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiGroups {
	@Bean
	fun managersOpenApi(): GroupedOpenApi = groupedOpenApi(
		group = "managers",
		displayName = "Managers",
		pathsToMatch = arrayOf("/v1/Protected/**"),
	)

	@Bean
	fun usersOpenApi(): GroupedOpenApi = groupedOpenApi(
		group = "users",
		displayName = "Users",
		pathsToMatch = arrayOf("/v1/Public/**"),
	)

	@Bean
	fun developersOpenApi(): GroupedOpenApi = groupedOpenApi(
		group = "developers",
		displayName = "Developers",
		pathsToMatch = arrayOf("/**"),
		pathsToExclude = arrayOf("/v1/Protected/**", "/v1/Public/**"),
	)

	private fun groupedOpenApi(
		group: String,
		displayName: String,
		pathsToMatch: Array<String>,
		pathsToExclude: Array<String> = emptyArray(),
	): GroupedOpenApi {
		val builder = GroupedOpenApi.builder()
			.group(group)
			.displayName(displayName)
			.pathsToMatch(*pathsToMatch)
		if (pathsToExclude.isNotEmpty()) {
			builder.pathsToExclude(*pathsToExclude)
		}
		return builder.build()
	}
}
