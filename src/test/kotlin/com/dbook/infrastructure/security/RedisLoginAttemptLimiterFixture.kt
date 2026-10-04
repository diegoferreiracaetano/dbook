package com.dbook.infrastructure.security

import com.dbook.AbstractIntegrationTest
import com.dbook.domain.identity.LoginAttemptLimiter
import org.springframework.beans.factory.annotation.Autowired

abstract class RedisLoginAttemptLimiterFixture : AbstractIntegrationTest() {
    @Autowired
    lateinit var limiter: LoginAttemptLimiter

    protected fun uniqueKey() = "email:limiter${(1..999_999_999).random()}@example.com"
}
