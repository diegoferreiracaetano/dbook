package com.dbook.infrastructure.web.airatelimitinterceptor

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AllowsRequestsWithinLimitAndBlocksTheNextWith429Test : AiRateLimitInterceptorFixture() {
    @Test
    fun `given 5 requests already made when a 6th request arrives then it is blocked with 429`() {
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
}
