package com.dbook.domain.appconfig

/** What the app of one platform must know before it starts: the oldest version still served and the newest one. */
data class PlatformConfig(
    val minSupportedVersion: String,
    val latestVersion: String,
    val storeUrl: String,
)

/** The server's say on which app versions it still serves: the app compares its own version and may ask to update. */
data class AppConfig(
    val android: PlatformConfig,
    val ios: PlatformConfig,
)

interface AppConfigSource {
    fun current(): AppConfig
}
