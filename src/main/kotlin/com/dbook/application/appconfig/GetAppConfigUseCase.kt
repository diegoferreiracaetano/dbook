package com.dbook.application.appconfig

import com.dbook.domain.appconfig.AppConfig
import com.dbook.domain.appconfig.AppConfigSource
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

@Observed(name = "dbook.usecase")
@Service
class GetAppConfigUseCase(
    private val source: AppConfigSource,
) {
    fun execute(): AppConfig = source.current()
}
