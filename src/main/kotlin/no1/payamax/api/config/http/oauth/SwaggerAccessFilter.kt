package no1.payamax.api.config.http.oauth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.access.ExceptionTranslationFilter
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.web.util.matcher.IpAddressMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Paths for Swagger UI and the OpenAPI documents, including grouped and YAML forms.
 */
internal val swaggerDocumentationPatterns: List<String> = listOf(
	"/swagger-ui.html",
	"/swagger-ui/**",
	"/v3/api-docs/**",
	"/v3/api-docs.*",
	"/v3/api-docs.*/**",
)

/**
 * Rejects Swagger UI and OpenAPI requests unless the client address is inside
 * [SwaggerAccessProperties.allowedIpRanges] or the caller has ROLE_ADMIN.
 * Each range is a single address or a CIDR prefix. An address that matches
 * none of them is logged, including when ROLE_ADMIN still allows the request.
 *
 * Register this after [ExceptionTranslationFilter]. An anonymous caller is then
 * asked to authenticate, and any other caller receives 403. The address compared
 * with the ranges is [HttpServletRequest.getRemoteAddr], which includes the client
 * address restored from forwarded headers.
 */
class SwaggerAccessFilter(
	properties: SwaggerAccessProperties,
) : OncePerRequestFilter() {
	init {
		properties.validate()
	}

	private val allowedRanges = properties.allowedRanges()
	private val ranges = allowedRanges.map { IpAddressMatcher(it) }
	private val documentation: RequestMatcher = OrRequestMatcher(
		swaggerDocumentationPatterns.map { PathPatternRequestMatcher.pathPattern(it) },
	)

	override fun shouldNotFilter(request: HttpServletRequest): Boolean = !documentation.matches(request)

	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		filterChain: FilterChain,
	) {
		if (fromAllowedNetwork(request) || hasAdminRole()) {
			filterChain.doFilter(request, response)
			return
		}
		log.warn(
			"Rejected {} {} from {} for Swagger UI or API docs",
			request.method,
			request.requestURI,
			request.remoteAddr,
		)
		throw AccessDeniedException("Swagger UI and API docs require an allowed network or ROLE_ADMIN.")
	}

	private fun fromAllowedNetwork(request: HttpServletRequest): Boolean {
		val remote = request.remoteAddr
		if (matches(remote)) {
			return true
		}
		val mapped = ipv4FromMappedAddress(remote)
		if (mapped != null && matches(mapped)) {
			return true
		}
		log.warn(
			"Swagger IP check failed for {} (IPv4-mapped {}); allowed ranges {}",
			remote,
			mapped ?: "n/a",
			allowedRanges,
		)
		return false
	}

	private fun matches(address: String): Boolean = ranges.any { it.matches(address) }

	private fun hasAdminRole(): Boolean {
		val authentication = SecurityContextHolder.getContext().authentication ?: return false
		return authentication.authorities.any { it.authority == ADMIN_ROLE }
	}

	private companion object {
		const val ADMIN_ROLE = "ROLE_ADMIN"
		val log = LoggerFactory.getLogger(SwaggerAccessFilter::class.java)

		fun ipv4FromMappedAddress(address: String?): String? {
			if (address == null) {
				return null
			}
			val prefix = "::ffff:"
			if (!address.startsWith(prefix, ignoreCase = true)) {
				return null
			}
			val mapped = address.substring(prefix.length)
			return mapped.takeIf { it.contains('.') }
		}
	}
}
