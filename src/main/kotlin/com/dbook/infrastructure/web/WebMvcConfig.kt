package com.dbook.infrastructure.web

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableConfigurationProperties(RequestLimitsProperties::class)
class WebMvcConfig(
    private val aiRateLimitInterceptor: AiRateLimitInterceptor,
    private val userRateLimitInterceptor: UserRateLimitInterceptor,
    private val verifiedEmailInterceptor: VerifiedEmailInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(userRateLimitInterceptor).addPathPatterns("/v1/**")
        registry.addInterceptor(aiRateLimitInterceptor).addPathPatterns("/v1/ai/**")
        registry.addInterceptor(verifiedEmailInterceptor)
            .addPathPatterns("/v1/bookings", "/v1/accommodations/*/bookings", "/v1/payments")
    }
}
