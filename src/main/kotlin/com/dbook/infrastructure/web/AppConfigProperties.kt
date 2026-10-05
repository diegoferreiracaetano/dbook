package com.dbook.infrastructure.web

import com.dbook.domain.appconfig.AppConfig
import com.dbook.domain.appconfig.AppConfigSource
import com.dbook.domain.appconfig.PlatformConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/** `app-config.android.*` and `app-config.ios.*`: the minimum is a configuration change, not a change of code. */
@ConfigurationProperties(prefix = "app-config")
data class AppConfigProperties(
    val android: Platform = Platform(),
    val ios: Platform = Platform(),
) {
    data class Platform(
        val minSupportedVersion: String = "0.0.0",
        val latestVersion: String = "0.0.0",
        val storeUrl: String = "",
    ) {
        fun toDomain() = PlatformConfig(minSupportedVersion, latestVersion, storeUrl)
    }
}

@Component
class PropertiesAppConfigSource(
    private val properties: AppConfigProperties,
) : AppConfigSource {
    override fun current() = AppConfig(properties.android.toDomain(), properties.ios.toDomain())
}
