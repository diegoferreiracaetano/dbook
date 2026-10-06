package com.dbook.presentation.common

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class ApiLifecycleConfig(
    private val meterRegistry: ObjectProvider<MeterRegistry>,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        // a web slice has no metrics registry: the headers still work, the count goes nowhere
        registry.addInterceptor(ApiLifecycleInterceptor(meterRegistry.getIfAvailable { SimpleMeterRegistry() }))
            .addPathPatterns("/v1/**", "/v2/**")
    }
}
