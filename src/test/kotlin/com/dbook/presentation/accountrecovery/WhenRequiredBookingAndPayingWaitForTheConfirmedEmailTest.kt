package com.dbook.presentation.accountrecovery

import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals

// the policy on: the other tests run with it off, so that the many that register and book right away keep working
@TestPropertySource(properties = ["account.require-verified-email=true"])
class WhenRequiredBookingAndPayingWaitForTheConfirmedEmailTest : AccountRecoveryApiFixture() {
    private val email = uniqueEmail()

    private fun bookWith(
        token: String,
        flightId: Long,
        seatId: Long,
    ) = postJson("/v1/bookings", mapOf("bookableId" to flightId, "seatId" to seatId), token)

    private fun confirm() {
        postJson("/v1/auth/verify-email", mapOf("token" to tokenMailedTo(email, CONFIRM)))
    }

    @Test
    fun `given an unconfirmed customer when they book then 403 EMAIL_NOT_VERIFIED and nothing is reserved`() {
        val session = registerAndLogin(email)
        val (flightId, seatId) = registerFlightWithOneSeat()

        val refused = bookWith(session, flightId, seatId)

        assertEquals(403, refused.response.status)
        assertEquals("EMAIL_NOT_VERIFIED", errorCodeOf(refused))
        assertEquals("AVAILABLE", seatStatus(seatId))
    }

    @Test
    fun `given an unconfirmed customer when they pay then 403 EMAIL_NOT_VERIFIED`() {
        val session = registerAndLogin(email)

        val refused = postJson("/v1/payments", mapOf("bookingIds" to listOf(1), "cardLast4" to "4242"), session)

        assertEquals(403, refused.response.status)
        assertEquals("EMAIL_NOT_VERIFIED", errorCodeOf(refused))
    }

    @Test
    fun `given an unconfirmed customer when they only look around then it works`() {
        val session = registerAndLogin(email)

        assertEquals(
            200,
            mockMvc.get("/v1/users/me") {
                header("Authorization", "Bearer $session")
            }.andReturn().response.status,
        )
        assertEquals(
            200,
            mockMvc.get("/v1/bookings") {
                header("Authorization", "Bearer $session")
            }.andReturn().response.status,
        )
    }

    @Test
    fun `given the link used when they book then it works, with the same session`() {
        val session = registerAndLogin(email)
        val (flightId, seatId) = registerFlightWithOneSeat()
        assertEquals(403, bookWith(session, flightId, seatId).response.status)

        confirm()

        assertEquals(201, bookWith(session, flightId, seatId).response.status)
    }

    private fun seatStatus(seatId: Long): String =
        jdbcTemplate.queryForObject("SELECT status FROM seat WHERE id = ?", String::class.java, seatId)
            ?: error("no seat")
}
