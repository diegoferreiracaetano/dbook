package com.dbook.presentation.promo

import com.dbook.domain.common.access.Role
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class ACodeThatCannotBeUsedIsRefusedAndTheBookingStaysPendingTest : PromoApiFixture() {
    private fun refusedWith(
        code: String,
        token: String,
        booking: Long,
        paymentsWithTheCode: Int = 0,
    ) {
        val paid = payWith(token, listOf(booking), code)

        assertEquals(422, paid.response.status)
        assertEquals("PROMO_REJECTED", json(paid)["code"].asText())
        assertEquals("PENDING", statusOfBooking(booking))
        assertEquals(
            paymentsWithTheCode,
            jdbcTemplate.queryForObject("SELECT count(*) FROM payment WHERE promo_code = ?", Int::class.java, code),
        )
    }

    @Test
    fun `given an expired code when paying then it is a 422 PROMO_REJECTED and the booking stays PENDING`() {
        val past = Instant.now().minus(2, ChronoUnit.DAYS)
        val (_, code) =
            newPromo(
                manager(),
                "validFrom" to past.toString(),
                "validUntil" to past.plusSeconds(60).toString(),
            )
        val (token, booking) = customerWithABooking()

        refusedWith(code, token, booking)
        assertEquals(422, preview(token, code, listOf(booking)).response.status)
    }

    @Test
    fun `given a deactivated code or one above the minimum when paying then it is refused`() {
        val team = manager()
        val (off, deactivated) = newPromo(team)
        adminPost(team, "/v1/admin/promo-codes/$off/deactivate")
        val (_, big) = newPromo(team, "minAmount" to 500)
        val (token, booking) = customerWithABooking()

        refusedWith(deactivated, token, booking)
        refusedWith(big, token, booking)
    }

    @Test
    fun `given an unknown code when paying or previewing then it is a 404 and the booking stays PENDING`() {
        val (token, booking) = customerWithABooking()

        assertEquals(404, payWith(token, listOf(booking), "NOSUCHCODE").response.status)
        assertEquals(404, preview(token, "NOSUCHCODE", listOf(booking)).response.status)
        assertEquals("PENDING", statusOfBooking(booking))
    }

    @Test
    fun `given a once-per-customer code when the same customer pays a second time with it then it is a 422`() {
        val (id, code) = newPromo(manager())
        val token = registerAndLogin(uniqueEmail())
        val (first, second) = bookSeats(token, 2)
        assertEquals(201, payWith(token, listOf(first), code).response.status)

        refusedWith(code, token, second, paymentsWithTheCode = 1)

        assertEquals(1, redeemed(id))
    }

    @Test
    fun `given someone else's booking or a staff-less preview when previewing then it is refused as usual`() {
        val (_, code) = newPromo(manager())
        val (_, booking) = customerWithABooking()
        val (stranger) = customerWithABooking()

        assertEquals(403, preview(stranger, code, listOf(booking)).response.status)
        assertEquals(
            403,
            registerStaffAndLogin(uniqueEmail(), Role.SUPPORT).let {
                payWith(it, listOf(booking), code).response.status
            },
        )
    }
}
