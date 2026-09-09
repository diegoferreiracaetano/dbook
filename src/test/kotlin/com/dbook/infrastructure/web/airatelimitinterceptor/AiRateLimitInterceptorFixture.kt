package com.dbook.infrastructure.web.airatelimitinterceptor

import com.dbook.infrastructure.web.AiRateLimitInterceptor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

abstract class AiRateLimitInterceptorFixture {
    protected val interceptor = AiRateLimitInterceptor()

    @BeforeTest
    fun authenticateAsUser42() {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken("42", null, emptyList())
    }

    @AfterTest
    fun clearAuthentication() {
        SecurityContextHolder.clearContext()
    }
}
