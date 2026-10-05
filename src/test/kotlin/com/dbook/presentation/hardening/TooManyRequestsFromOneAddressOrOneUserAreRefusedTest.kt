package com.dbook.presentation.hardening

import com.dbook.presentation.securityintegration.SecurityIntegrationFixture
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@TestPropertySource(
    properties = [
        "request-limits.rate-limit-enabled=true",
        "request-limits.per-ip-per-minute=20",
        "request-limits.per-user-per-minute=10",
    ],
)
class TooManyRequestsFromOneAddressOrOneUserAreRefusedTest : SecurityIntegrationFixture() {
    private fun anAddress() = "10.${(1..250).random()}.${(1..250).random()}.${(1..250).random()}"

    private fun statusFrom(
        address: String,
        token: String? = null,
    ) = mockMvc.get("/v1/bookings") {
        token?.let { header("Authorization", "Bearer $it") }
        with {
            it.remoteAddr = address
            it
        }
    }.andReturn().response.status

    @Test
    fun `given one address when it makes 21 requests in a minute then the 21st is a 429 with Retry-After`() {
        val address = anAddress()

        repeat(20) { assertEquals(401, statusFrom(address)) }
        val refused =
            mockMvc.get("/v1/bookings") {
                with {
                    it.remoteAddr = address
                    it
                }
            }.andReturn().response

        assertEquals(429, refused.status)
        assertNotNull(refused.getHeader("Retry-After"))
        assertEquals("RATE_LIMITED", objectMapper.readTree(refused.contentAsString)["code"].asText())
        assertEquals(401, statusFrom(anAddress()), "another address is not affected")
    }

    @Test
    fun `given one signed-in user when they make 11 requests from different addresses then the 11th is a 429`() {
        val token = registerAndLogin(uniqueEmail())

        repeat(10) { assertEquals(200, statusFrom(anAddress(), token)) }

        assertEquals(429, statusFrom(anAddress(), token))
        assertEquals(200, statusFrom(anAddress(), registerAndLogin(uniqueEmail())), "another user is not affected")
    }
}
