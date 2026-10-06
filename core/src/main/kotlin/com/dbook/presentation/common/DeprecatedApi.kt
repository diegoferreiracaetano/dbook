package com.dbook.presentation.common

/**
 * Marks an endpoint (or a whole controller) as on its way out. Every response then carries `Deprecation`, `Sunset` and
 * `Link` (RFC 9745 and RFC 8594), and each call is counted in `dbook.api.deprecated.calls` by path and app version:
 * the day that metric is zero, the endpoint can go.
 *
 * @property since the day it was deprecated (ISO date)
 * @property sunset the day it will stop answering (ISO date): never earlier than a client can reasonably update
 * @property link where the replacement and the migration are explained
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DeprecatedApi(
    val since: String,
    val sunset: String,
    val link: String = "",
)
