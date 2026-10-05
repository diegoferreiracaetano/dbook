package com.dbook.infrastructure.web

import org.springframework.boot.context.properties.ConfigurationProperties

/** `request-limits.*`: how big a body may be, and how many requests a client may make in a minute. */
@ConfigurationProperties(prefix = "request-limits")
data class RequestLimitsProperties(
    val maxBodyBytes: Long = DEFAULT_BODY_BYTES,
    val maxImportBodyBytes: Long = DEFAULT_IMPORT_BYTES,
    val rateLimitEnabled: Boolean = true,
    val perIpPerMinute: Long = DEFAULT_PER_IP,
    val perUserPerMinute: Long = DEFAULT_PER_USER,
) {
    private companion object {
        const val DEFAULT_BODY_BYTES = 256L * 1024
        const val DEFAULT_IMPORT_BYTES = 5L * 1024 * 1024
        const val DEFAULT_PER_IP = 300L
        const val DEFAULT_PER_USER = 600L
    }
}
