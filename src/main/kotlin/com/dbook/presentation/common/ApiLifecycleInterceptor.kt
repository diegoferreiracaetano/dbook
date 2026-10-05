package com.dbook.presentation.common

import com.dbook.domain.common.AppClient
import io.micrometer.core.instrument.MeterRegistry
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.HandlerMapping
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Tells a client, on every call, that the endpoint it just used is deprecated: the three headers of [DeprecatedApi],
 * and a count of the call (by the endpoint's path pattern, never the real path, so an id does not become a series).
 */
class ApiLifecycleInterceptor(
    private val meterRegistry: MeterRegistry,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val deprecated = (handler as? HandlerMethod)?.let { annotationOf(it) }
        if (deprecated != null) {
            response.setHeader("Deprecation", "@${epochSecondsOf(deprecated.since)}")
            response.setHeader("Sunset", HTTP_DATE.format(startOfDay(deprecated.sunset)))
            if (deprecated.link.isNotBlank()) {
                response.addHeader("Link", "<${deprecated.link}>; rel=\"deprecation\"")
            }
            val path = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)?.toString() ?: "unknown"
            val version = AppClient.version(request.getHeader(AppClient.VERSION_HEADER))
            meterRegistry.counter("dbook.api.deprecated.calls", "path", path, "appVersion", version).increment()
        }
        return true
    }

    private fun annotationOf(handler: HandlerMethod): DeprecatedApi? =
        AnnotatedElementUtils.findMergedAnnotation(handler.method, DeprecatedApi::class.java)
            ?: AnnotatedElementUtils.findMergedAnnotation(handler.beanType, DeprecatedApi::class.java)

    private fun startOfDay(date: String) = LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC)

    private fun epochSecondsOf(date: String) = startOfDay(date).toEpochSecond()

    private companion object {
        // an HTTP date (RFC 9110): two digits for the day, always in GMT, English names
        val HTTP_DATE: DateTimeFormatter =
            DateTimeFormatter.ofPattern(
                "EEE, dd MMM yyyy HH:mm:ss 'GMT'",
                Locale.ENGLISH,
            )
    }
}
