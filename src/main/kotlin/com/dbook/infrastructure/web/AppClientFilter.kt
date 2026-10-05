package com.dbook.infrastructure.web

import com.dbook.domain.common.AppClient
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.beans.factory.ObjectProvider
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Knows which app is calling: the version and the platform the app sends (`X-App-Version`, `X-App-Platform`) go to the
 * log (the `appVersion` and `appPlatform` fields of every line of the request) and to a metric,
 * `dbook.app.requests{platform,appVersion}`, which is what answers "can the 1.2 app be switched off yet?": the day
 * its calls reach zero. Both are normalized to a small closed set (see [AppClient]), so the metric cannot blow up.
 *
 * Ordered after the request logging filter, so its MDC is already open.
 */
@Component
@Order(AppClientFilter.ORDER)
class AppClientFilter(
    meterRegistryProvider: ObjectProvider<MeterRegistry>,
) : OncePerRequestFilter() {
    // a web slice has no metrics registry: the log fields still work, the count goes nowhere
    private val meterRegistry: MeterRegistry = meterRegistryProvider.getIfAvailable { SimpleMeterRegistry() }

    // the docs and the actuator are not the app
    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.requestURI.startsWith("/swagger-ui") || request.requestURI.startsWith("/v3/api-docs")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val version = AppClient.version(request.getHeader(AppClient.VERSION_HEADER))
        val platform = AppClient.platform(request.getHeader(AppClient.PLATFORM_HEADER))
        MDC.put(VERSION_KEY, version)
        MDC.put(PLATFORM_KEY, platform)
        meterRegistry.counter("dbook.app.requests", "platform", platform, "appVersion", version).increment()
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(VERSION_KEY)
            MDC.remove(PLATFORM_KEY)
        }
    }

    companion object {
        // right after the request logging filter (HIGHEST_PRECEDENCE + 2)
        const val ORDER = Ordered.HIGHEST_PRECEDENCE + 3
        private const val VERSION_KEY = "appVersion"
        private const val PLATFORM_KEY = "appPlatform"
    }
}
