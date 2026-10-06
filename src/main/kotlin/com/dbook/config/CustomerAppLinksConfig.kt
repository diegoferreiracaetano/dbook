package com.dbook.config

import com.dbook.application.identity.CustomerAppLinks
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CustomerAppLinksConfig {
    @Bean
    fun customerAppLinks(
        @Value("\${customer-app.base-url}") baseUrl: String,
    ) = CustomerAppLinks(baseUrl.trimEnd('/'))
}
