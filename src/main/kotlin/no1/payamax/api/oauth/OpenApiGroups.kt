package no1.payamax.api.oauth

import io.swagger.v3.oas.models.OpenAPI
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.Locale

@Configuration
class OpenApiGroups {
	@Bean
	fun managersOpenApi(): GroupedOpenApi = groupedOpenApi(
		group = "managers",
		displayName = "Managers",
	) { path -> path.matchesPrefix("/v1/Protected") }

	@Bean
	fun usersOpenApi(): GroupedOpenApi = groupedOpenApi(
		group = "users",
		displayName = "Users",
	) { path -> path.matchesPrefix("/v1/Public") }

	@Bean
	fun developersOpenApi(): GroupedOpenApi = groupedOpenApi(
		group = "developers",
		displayName = "Developers",
	) { path -> !path.matchesPrefix("/v1/Protected") && !path.matchesPrefix("/v1/Public") }

	private fun groupedOpenApi(
		group: String,
		displayName: String,
		include: (String) -> Boolean,
	): GroupedOpenApi {
		return GroupedOpenApi.builder()
			.group(group)
			.displayName(displayName)
			.pathsToMatch("/**")
			.addOpenApiCustomizer(retainPaths(include))
			.build()
	}

	private fun retainPaths(include: (String) -> Boolean): OpenApiCustomizer {
		return OpenApiCustomizer { openApi -> openApi.retainPaths(include) }
	}
}

private fun OpenAPI.retainPaths(include: (String) -> Boolean) {
	val paths = this.paths ?: return
	paths.keys.filterNot(include).forEach { paths.remove(it) }
}

private fun String.matchesPrefix(prefix: String): Boolean {
	val path = lowercase(Locale.ROOT)
	val needle = prefix.trimEnd('/').lowercase(Locale.ROOT)
	return path == needle || path.startsWith("$needle/")
}
