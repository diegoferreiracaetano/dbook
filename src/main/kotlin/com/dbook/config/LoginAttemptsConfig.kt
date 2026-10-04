package com.dbook.config

import com.dbook.application.identity.LoginAttemptsPolicy
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LoginAttemptsConfig {
    @Bean
    fun loginAttemptsPolicy(
        @Value("\${login-attempts.max-failures-per-email}") maxFailuresPerEmail: Int,
        @Value("\${login-attempts.max-failures-per-ip}") maxFailuresPerIp: Int,
    ) = LoginAttemptsPolicy(maxFailuresPerEmail, maxFailuresPerIp)
}
