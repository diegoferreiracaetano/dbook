package com.dbook.config

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.models.Operation
import org.springdoc.core.customizers.GlobalOpenApiCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Declares the `bearerAuth` scheme (the "Authorize" button in Swagger UI) and centralizes
 * `@RequestParam`/`@PathVariable` examples — kept out of controllers on purpose, see
 * [parameterExamples].
 */
@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
)
class OpenApiConfig {
    // One Swagger group per API version (the UI gets a selector), plus the operational endpoints.
    @Bean
    fun v1Api(): GroupedOpenApi = GroupedOpenApi.builder().group("v1").pathsToMatch("/v1/**").build()

    @Bean
    fun operationalApi(): GroupedOpenApi = GroupedOpenApi.builder().group("operational").pathsToMatch("/health").build()

    // GlobalOpenApiCustomizer, not a plain OpenApiCustomizer: once the docs are split into groups,
    // only the global kind is applied to every group.
    @Bean
    fun parameterExamples(): GlobalOpenApiCustomizer =
        GlobalOpenApiCustomizer { openApi ->
            openApi.paths["/v1/flights/search"]?.get?.apply {
                exampleFor("origin", "GRU")
                exampleFor("destination", "GIG")
                exampleFor("date", "2026-10-01")
            }
            openApi.paths["/v1/bookings/{id}/cancel"]?.post?.exampleFor("id", 1)
            openApi.paths["/v1/bookables/{id}/seats"]?.get?.exampleFor("id", 1)
            openApi.paths["/v1/payments"]?.post?.exampleFor("Idempotency-Key", "3f2b8c1e-6a4d-4e7a-9d1b-5c8e2a7f0b94")
        }

    private fun Operation.exampleFor(
        parameterName: String,
        example: Any,
    ) {
        parameters?.find { it.name == parameterName }?.example = example
    }
}
