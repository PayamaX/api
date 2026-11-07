package no1.payamax.api.config.http.oauth

import jakarta.servlet.http.HttpServletRequest
import org.springdoc.core.properties.SwaggerUiConfigProperties
import org.springdoc.core.properties.SwaggerUiOAuthProperties
import org.springdoc.core.providers.ObjectMapperProvider
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer
import org.springdoc.webmvc.ui.SwaggerIndexTransformer
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.web.servlet.resource.ResourceTransformerChain
import org.springframework.web.servlet.resource.TransformedResource
import java.nio.charset.StandardCharsets
import kotlin.text.iterator

@Configuration
class SwaggerUiLogoutConfig(
	private val oauth2: OAuth2Properties,
) {
	@Bean
	fun indexPageTransformer(
		swaggerUiConfig: SwaggerUiConfigProperties,
		swaggerUiOAuthProperties: SwaggerUiOAuthProperties,
		swaggerWelcomeCommon: SwaggerWelcomeCommon,
		objectMapperProvider: ObjectMapperProvider,
	): SwaggerIndexTransformer {
		return SwaggerUiLogoutIndexTransformer(
			swaggerUiConfig,
			swaggerUiOAuthProperties,
			swaggerWelcomeCommon,
			objectMapperProvider,
			oauth2,
		)
	}
}

/**
 * Adds an identity-provider logout link to the Swagger UI authorize dialog.
 * Swagger's own Logout button only drops the token stored in the page.
 */
class SwaggerUiLogoutIndexTransformer(
	swaggerUiConfig: SwaggerUiConfigProperties,
	swaggerUiOAuthProperties: SwaggerUiOAuthProperties,
	swaggerWelcomeCommon: SwaggerWelcomeCommon,
	objectMapperProvider: ObjectMapperProvider,
	private val oauth2: OAuth2Properties,
) : SwaggerIndexPageTransformer(
	swaggerUiConfig,
	swaggerUiOAuthProperties,
	swaggerWelcomeCommon,
	objectMapperProvider,
) {
	override fun transform(
		request: HttpServletRequest,
		resource: Resource,
		transformerChain: ResourceTransformerChain,
	): Resource {
		val transformed = super.transform(request, resource, transformerChain)
		if (resource.filename != "swagger-initializer.js") {
			return transformed
		}
		val script = transformed.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
		val updated = injectIdpLogoutPlugin(script, oauth2.endSessionUrl())
		return TransformedResource(resource, updated.toByteArray(StandardCharsets.UTF_8))
	}
}

internal fun injectIdpLogoutPlugin(initializerJs: String, logoutUrl: String): String {
	val pluginMarker = "SwaggerUIBundle.plugins.DownloadUrl"
	val bundleMarker = "window.ui = SwaggerUIBundle({"
	require(initializerJs.occurrences(pluginMarker) == 1) {
		"Swagger UI initializer is missing the plugins list."
	}
	require(initializerJs.occurrences(bundleMarker) == 1) {
		"Swagger UI initializer is missing the SwaggerUIBundle setup."
	}
	return initializerJs
		.replace(pluginMarker, "$pluginMarker,\n      idpLogoutPlugin")
		.replace(bundleMarker, idpLogoutPluginSource(logoutUrl) + bundleMarker)
}

private fun idpLogoutPluginSource(logoutUrl: String): String {
	val literal = logoutUrl.toJavaScriptStringLiteral()
	return """
		function idpLogoutPlugin() {
		  var logoutUrl = $literal;
		  return {
		    wrapComponents: {
		      oauth2: function (Original, system) {
		        var React = system.React;
		        return function (props) {
		          return React.createElement(
		            "div",
		            null,
		            React.createElement(Original, props),
		            React.createElement(
		              "p",
		              null,
		              React.createElement(
		                "a",
		                {
		                  href: logoutUrl,
		                  onClick: function (event) {
		                    event.preventDefault();
		                    try {
		                      props.errActions.clear({ authId: props.name, type: "auth", source: "auth" });
		                      props.authActions.logoutWithPersistOption([props.name]);
		                    } finally {
		                      window.location.assign(logoutUrl);
		                    }
		                  }
		                },
		                "Logout from identity provider"
		              )
		            )
		          );
		        };
		      }
		    }
		  };
		}

	""".trimIndent() + "\n"
}

private fun String.toJavaScriptStringLiteral(): String {
	val escaped = buildString {
		for (ch in this@toJavaScriptStringLiteral) {
			when (ch) {
				'\\' -> append("\\\\")
				'"' -> append("\\\"")
				'\n' -> append("\\n")
				'\r' -> append("\\r")
				'<' -> append("\\u003c")
				'>' -> append("\\u003e")
				'\u2028' -> append("\\u2028")
				'\u2029' -> append("\\u2029")
				else -> append(ch)
			}
		}
	}
	return "\"$escaped\""
}

private fun String.occurrences(needle: String): Int {
	var count = 0
	var from = 0
	while (true) {
		val index = indexOf(needle, from)
		if (index < 0) {
			return count
		}
		count++
		from = index + needle.length
	}
}
