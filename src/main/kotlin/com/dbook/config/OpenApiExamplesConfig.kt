package com.dbook.config

import io.swagger.v3.oas.models.Operation
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiExamplesConfig {
    @Bean
    fun parameterExamples(): OpenApiCustomizer =
        OpenApiCustomizer { openApi ->
            openApi.paths["/flights/search"]?.get?.apply {
                exampleFor("origin", "GRU")
                exampleFor("destination", "GIG")
                exampleFor("date", "2026-10-01")
            }
            openApi.paths["/bookings/{id}/cancel"]?.post?.exampleFor("id", 1)
        }

    private fun Operation.exampleFor(
        parameterName: String,
        example: Any,
    ) {
        parameters?.find { it.name == parameterName }?.example = example
    }
}
