package com.dbook.presentation.appconfig

import com.dbook.application.appconfig.GetAppConfigUseCase
import com.dbook.domain.appconfig.PlatformConfig
import com.dbook.presentation.common.ApiPaths
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class PlatformConfigResponse(
    @get:Schema(example = "1.2.0", description = "the oldest app version still served: below it the app asks to update")
    val minSupportedVersion: String,
    @get:Schema(example = "1.5.0")
    val latestVersion: String,
    @get:Schema(description = "where the user updates the app")
    val storeUrl: String,
) {
    companion object {
        fun from(config: PlatformConfig) =
            PlatformConfigResponse(config.minSupportedVersion, config.latestVersion, config.storeUrl)
    }
}

data class AppConfigResponse(
    val android: PlatformConfigResponse,
    val ios: PlatformConfigResponse,
)

/** `GET /app-config` — public: the app asks it before anything else, even before the user signs in. */
@RestController
@RequestMapping("${ApiPaths.V1}/app-config")
@Tag(name = "App config (public)", description = "Which app versions the server still serves")
class AppConfigController(
    private val getAppConfigUseCase: GetAppConfigUseCase,
) {
    @Operation(
        summary = "The oldest app version served and the newest, per platform",
        description =
            "The app compares its own version with `minSupportedVersion` and shows the \"update the app\" screen " +
                "when it is below. Send `X-App-Version` and `X-App-Platform` on every call: they are what tells us " +
                "when an old version can be switched off.",
    )
    @GetMapping
    fun get(): AppConfigResponse {
        val config = getAppConfigUseCase.execute()
        return AppConfigResponse(PlatformConfigResponse.from(config.android), PlatformConfigResponse.from(config.ios))
    }
}
