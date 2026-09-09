package com.dbook.infrastructure.web

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiRateLimitInterceptorTest {
    private val interceptor = AiRateLimitInterceptor()

    @BeforeTest
    fun authenticateAsUser42() {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken("42", null, emptyList())
    }

    @AfterTest
    fun clearAuthentication() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `allows requests within the limit and blocks the one after it with 429`() {
        repeat(5) {
            val response = MockHttpServletResponse()
            val allowed = interceptor.preHandle(MockHttpServletRequest(), response, Any())
            assertTrue(allowed, "request ${it + 1} should have been allowed")
        }

        val sixthResponse = MockHttpServletResponse()
        val sixthAllowed = interceptor.preHandle(MockHttpServletRequest(), sixthResponse, Any())

        assertTrue(!sixthAllowed)
        assertEquals(429, sixthResponse.status)
        assertTrue(sixthResponse.contentAsString.contains("Rate limit exceeded"))
    }

    @Test
    fun `tracks limits independently per user`() {
        repeat(5) { interceptor.preHandle(MockHttpServletRequest(), MockHttpServletResponse(), Any()) }

        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken("99", null, emptyList())
        val response = MockHttpServletResponse()
        val allowed = interceptor.preHandle(MockHttpServletRequest(), response, Any())

        assertTrue(allowed, "a different user should have its own, unconsumed bucket")
    }
}
