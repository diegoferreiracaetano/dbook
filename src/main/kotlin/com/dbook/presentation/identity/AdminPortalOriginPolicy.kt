package com.dbook.presentation.identity

import com.dbook.config.CorsProperties
import com.dbook.presentation.common.ForbiddenOriginException
import org.springframework.stereotype.Component
import org.springframework.web.cors.CorsConfiguration

// Browsers always send the Origin of the page; a missing or foreign one is refused.
// It reuses the CORS list (cors.allowed-origins), so there is a single place to change.
@Component
class AdminPortalOriginPolicy(
    corsProperties: CorsProperties,
) {
    private val allowed = CorsConfiguration().apply { allowedOriginPatterns = corsProperties.allowedOrigins }

    fun require(origin: String?) {
        if (origin == null || allowed.checkOrigin(origin) == null) {
            throw ForbiddenOriginException()
        }
    }
}
