package com.dbook.config

import com.dbook.application.identity.TwoFactorPolicy
import com.dbook.domain.identity.Role
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TwoFactorConfig {
    @Bean
    fun twoFactorPolicy(
        @Value("\${admin.two-factor.required-roles:}") requiredRoles: Set<String>,
    ) = TwoFactorPolicy(requiredRoles.filter { it.isNotBlank() }.map { Role.valueOf(it.trim().uppercase()) }.toSet())
}
