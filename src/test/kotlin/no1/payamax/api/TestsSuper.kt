package no1.payamax.api

import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.ImagePullPolicy
import org.testcontainers.images.PullPolicy
import org.testcontainers.lifecycle.Startables
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.function.Consumer

private object NeverPull : ImagePullPolicy {
	override fun shouldPull(imageName: DockerImageName): Boolean = false
}

private class ImagePull(
	val policy: ImagePullPolicy,
	val description: String,
)

private class BindHostPort(
	private val hostPort: Int,
	private val containerPort: Int,
) : Consumer<CreateContainerCmd> {
	override fun accept(cmd: CreateContainerCmd) {
		val hostConfig = cmd.hostConfig ?: HostConfig.newHostConfig()
		cmd.withHostConfig(hostConfig.withPortBindings(PortBinding.parse("$hostPort:$containerPort")))
	}
}

/**
 * Postgres and Keycloak from the services images (`ghcr.io/payamax/pgxdb-go` and
 * `ghcr.io/payamax/kcxkc-go`). On the `main` branch the tag is always `main`.
 * Otherwise it is `PAX_TAG` from the environment, then from the nearest `.env`
 * in the current directory or its ancestors, and finally the current git branch.
 *
 * `PAX_TC_PULL_POLICY` selects the image pull policy: `always`, `never`, or `default`.
 * An unset or blank variable is the same as `default`, which is the Testcontainers
 * policy and pulls an image when it is missing locally.
 *
 * Started once for the test run and kept until the JVM exits, so every test
 * class shares the same database and Keycloak.
 */
abstract class TestsSuper {
	protected fun readerToken(): String = accessToken(REALM, TRUSTED_CLIENT, USERNAME, PASSWORD)

	protected fun untrustedToken(): String = accessToken(REALM, UNTRUSTED_CLIENT, USERNAME, PASSWORD)

	companion object {
		private const val REALM = "pax"
		private const val TRUSTED_CLIENT = "SwaggerUI"
		private const val UNTRUSTED_CLIENT = "admin-cli"
		private const val USERNAME = "user@pax.ir"
		private const val PASSWORD = "password"
		private val DOTENV_PAX_TAG = Regex("""(?:export\s+)?PAX_TAG\s*=\s*(.*)""")

		private val tag = imageTag()
		private val imagePull = resolveImagePull()
		private val network: Network = Network.newNetwork()
		private val keycloakPort = freePort()
		private val http: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

		private val postgres: PostgreSQLContainer = PostgreSQLContainer(
			DockerImageName.parse("ghcr.io/payamax/pgxdb-go:$tag").asCompatibleSubstituteFor("postgres"),
		)
			.withImagePullPolicy(imagePull.policy)
			.withDatabaseName("pg")
			.withUsername("pg")
			.withPassword("password")
			.withNetwork(network)
			.withNetworkAliases("pgdb")
			.waitingFor(
				Wait.forLogMessage(".*Flyway completed\\..*", 1)
					.withStartupTimeout(Duration.ofMinutes(3)),
			)

		private val keycloak: GenericContainer<*> = GenericContainer(
			DockerImageName.parse("ghcr.io/payamax/kcxkc-go:$tag"),
		)
			.withImagePullPolicy(imagePull.policy)
			.withNetwork(network)
			.withImagePullPolicy(PullPolicy.defaultPolicy())
			.withEnv("KC_HOSTNAME", "localhost")
			.withEnv("KC_HOSTNAME_PORT", keycloakPort.toString())
			.withEnv("KC_HOSTNAME_STRICT", "false")
			.withEnv("KC_HOSTNAME_STRICT_HTTPS", "false")
			.withEnv("KC_PROXY", "none")
			.withEnv("KC_HTTP_ENABLED", "true")
			.withExposedPorts(8080)
			.withCreateContainerCmdModifier(BindHostPort(keycloakPort, 8080))
			.dependsOn(postgres)
			.waitingFor(
				Wait.forHttp("/health/ready")
					.forPort(8080)
					.forStatusCode(200)
					.withStartupTimeout(Duration.ofMinutes(3)),
			)

		init {
			try {
				Startables.deepStart(postgres, keycloak).join()
			} catch (failure: Throwable) {
				throw IllegalStateException(startupFailure(failure), failure)
			}
		}

		@DynamicPropertySource
		@JvmStatic
		fun containerProperties(registry: DynamicPropertyRegistry) {
			val issuer = "${authServerUrl()}/realms/$REALM"
			registry.add("oauth2.public-realm-url") { issuer }
			registry.add("oauth2.private-realm-url") { issuer }
			registry.add("oauth2.health-url") { "${authServerUrl()}/health/ready" }
			registry.add("oauth2.issuers[0]") { issuer }
			registry.add("oauth2.requires-secure-metadata") { "false" }
			registry.add("http-log.enabled") { "false" }
			registry.add("PAX_ADMIN_URL") { "http://localhost:5173" }
			registry.add("spring.r2dbc.url") { r2dbcUrl("pax") }
			registry.add("spring.r2dbc.username") { "pax_dml_api" }
			registry.add("spring.r2dbc.password") { "password" }
			registry.add("spring.flyway.url") { jdbcUrl("pax") }
			registry.add("spring.flyway.user") { "pax_ddl_fw" }
			registry.add("spring.flyway.password") { "password" }
			registry.add("spring.datasource.url") { jdbcUrl("pax") }
			registry.add("spring.datasource.username") { "pax_dml_api" }
			registry.add("spring.datasource.password") { "password" }
		}

		private fun accessToken(realm: String, clientId: String, username: String, password: String): String {
			val body = listOf(
				"grant_type" to "password",
				"client_id" to clientId,
				"username" to username,
				"password" to password,
			).joinToString("&") { (key, value) -> "$key=${encode(value)}" }
			val request = HttpRequest.newBuilder(URI.create("${authServerUrl()}/realms/$realm/protocol/openid-connect/token"))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.timeout(Duration.ofSeconds(20))
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build()
			val response = http.send(request, HttpResponse.BodyHandlers.ofString())
			if (response.statusCode() !in 200..299) {
				throw IllegalStateException(
					"Token request for client $clientId on realm $realm failed with ${response.statusCode()}: ${response.body()}",
				)
			}
			return Regex(""""access_token"\s*:\s*"([^"]+)"""").find(response.body())?.groupValues?.get(1)
				?: throw IllegalStateException("Token response for client $clientId did not include access_token.")
		}

		private fun authServerUrl(): String = "http://localhost:$keycloakPort"

		private fun jdbcUrl(database: String): String =
			"jdbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/$database"

		private fun r2dbcUrl(database: String): String =
			"r2dbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/$database"

		private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

		private fun freePort(): Int = java.net.ServerSocket(0).use { it.localPort }

		private fun startupFailure(failure: Throwable): String {
			val postgresImage = "ghcr.io/payamax/pgxdb-go:$tag"
			val keycloakImage = "ghcr.io/payamax/kcxkc-go:$tag"
			return buildString {
				appendLine("Failed to start the Postgres and Keycloak containers used by tests.")
				appendLine("Postgres image: $postgresImage (${localImage(postgresImage)})")
				appendLine("Keycloak image: $keycloakImage (${localImage(keycloakImage)})")
				appendLine("Pull policy: ${imagePull.description}")
				append(causeChain(failure))
			}
		}

		private fun localImage(image: String): String = runCatching {
			val process = ProcessBuilder("docker", "image", "inspect", "--format", "{{.Id}}", image)
				.redirectErrorStream(true)
				.start()
			val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
			if (process.waitFor() == 0 && output.isNotEmpty()) "present locally" else "not present locally: $output"
		}.getOrElse { error -> "could not inspect: ${error.javaClass.simpleName}: ${error.message}" }

		private fun causeChain(failure: Throwable): String {
			val seen = mutableSetOf<Throwable>()
			val lines = mutableListOf<String>()
			var current: Throwable? = failure
			while (current != null && seen.add(current)) {
				val frame = current.stackTrace.firstOrNull()
				val where = if (frame == null) "" else " (${frame.fileName}:${frame.lineNumber})"
				lines += "${current.javaClass.name}$where: ${current.message}"
				current = current.cause
			}
			return lines.joinToString(separator = "\n")
		}

		private fun resolveImagePull(): ImagePull {
			val configured = System.getenv("PAX_TC_PULL_POLICY")?.trim().orEmpty()
			return when (configured.lowercase()) {
				"", "default" -> ImagePull(
					PullPolicy.defaultPolicy(),
					"default. Testcontainers pulls an image when it is missing locally.",
				)
				"always" -> ImagePull(
					PullPolicy.alwaysPull(),
					"always. Testcontainers pulls an image even when a local copy exists.",
				)
				"never" -> ImagePull(
					NeverPull,
					"never. Testcontainers does not pull, so a missing image fails while creating the container.",
				)
				else -> throw IllegalArgumentException(
					"PAX_TC_PULL_POLICY must be always, never, or default (got \"$configured\").",
				)
			}
		}

		private fun imageTag(): String {
			val branch = gitBranch()
			if (branch == "main") return "main"

			System.getenv("PAX_TAG")?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
			dotEnvTag()?.let { return it }
			return branch.takeIf { it.isNotBlank() }
				?: throw IllegalStateException("PAX_TAG is not set and the current git branch could not be determined.")
		}

		private fun dotEnvTag(): String? = generateSequence(Path.of("").toAbsolutePath().normalize()) { it.parent }
			.map { it.resolve(".env") }.firstNotNullOfOrNull(::dotEnvTagFrom)

		private fun dotEnvTagFrom(path: Path): String? = runCatching {
			if (!Files.isRegularFile(path)) return@runCatching null

			Files.newBufferedReader(path).useLines { lines ->
				lines.firstNotNullOfOrNull { line ->
					val match = DOTENV_PAX_TAG.matchEntire(line.trim()) ?: return@firstNotNullOfOrNull null
					match.groupValues[1]
						.trim()
						.removeSurrounding("\"")
						.removeSurrounding("'")
						.takeIf { it.isNotBlank() }
				}
			}
		}.getOrNull()

		private fun gitBranch(): String = runCatching {
			val process = ProcessBuilder("git", "branch", "--show-current")
				.redirectError(ProcessBuilder.Redirect.DISCARD)
				.start()
			val branch = process.inputStream.bufferedReader().use { it.readText() }.trim()
			if (process.waitFor() == 0) branch else ""
		}.getOrDefault("")
	}
}
