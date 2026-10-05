package com.dbook.domain.common

/**
 * What the server learns about the app calling it, from the `X-App-Version` and `X-App-Platform` headers. Both are
 * **normalized into a small closed set** before they become a metric tag or a log field: a header is whatever the
 * caller wrote, and every distinct value would be a new time series (a hostile client could mint millions).
 */
object AppClient {
    const val VERSION_HEADER = "X-App-Version"
    const val PLATFORM_HEADER = "X-App-Platform"
    const val UNKNOWN = "unknown"

    private val PLATFORMS = setOf("android", "ios", "web")
    private val VERSION = Regex("^(\\d{1,3})\\.(\\d{1,3})(?:\\.\\d{1,5})?(?:[-+][0-9A-Za-z.-]{0,30})?$")

    /** "1.4.2+17" becomes "1.4": the major and the minor are enough to decide when a version can die. */
    fun version(raw: String?): String =
        raw?.trim()?.let { VERSION.matchEntire(it) }?.let { "${it.groupValues[1]}.${it.groupValues[2]}" } ?: UNKNOWN

    fun platform(raw: String?): String = raw?.trim()?.lowercase()?.takeIf { it in PLATFORMS } ?: UNKNOWN
}
