package com.dbook.config

import org.springframework.boot.context.properties.ConfigurationProperties

// bound by Spring Boot's binder: @Value does not split a comma-separated list into a Kotlin List<String>
@ConfigurationProperties(prefix = "cors")
data class CorsProperties(
    val allowedOrigins: List<String>,
)
