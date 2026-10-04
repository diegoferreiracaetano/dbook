package com.dbook.config

import com.dbook.application.identity.AdminPortalLinks
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AdminPortalLinksConfig {
    @Bean
    fun adminPortalLinks(
        @Value("\${admin-portal.base-url}") baseUrl: String,
    ) = AdminPortalLinks(baseUrl.trimEnd('/'))
}
