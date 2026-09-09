package com.dbook.infrastructure.web.airatelimitinterceptor

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.Test
import kotlin.test.assertTrue

class TracksLimitsIndependentlyPerUserTest : AiRateLimitInterceptorFixture() {
    @Test
    fun `given user 42 exhausted their limit when a different user requests then it is allowed`() {
        repeat(5) { interceptor.preHandle(MockHttpServletRequest(), MockHttpServletResponse(), Any()) }

        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken("99", null, emptyList())
        val response = MockHttpServletResponse()
        val allowed = interceptor.preHandle(MockHttpServletRequest(), response, Any())

        assertTrue(allowed, "a different user should have its own, unconsumed bucket")
    }
}
