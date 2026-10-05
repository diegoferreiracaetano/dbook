package com.dbook.presentation.adminbookings

import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class InsideTheLastDayOnlyASuperAdminWithAnOverrideAndANoteCanRefundTest : AdminBookingsFixture() {
    @Test
    fun `given a flight in 2 hours when refunding then 409, 403 with override, 400 without a note, 201 if allowed`() {
        val (_, booking) = paidBookingDepartingIn(hours = 2)
        val support = staff()
        val admin = staff(Role.SUPER_ADMIN)
        val override = mapOf("reason" to "FLIGHT_CANCELLED", "override" to true)

        val closed = refund(support, booking)
        assertEquals(409, closed.response.status)
        assertEquals("REFUND_WINDOW_CLOSED", errorCodeOf(closed))
        assertEquals(403, refund(support, booking, body = override + ("note" to "airline cancelled")).response.status)
        assertEquals(400, refund(admin, booking, body = override).response.status)

        val allowed = refund(admin, booking, body = override + ("note" to "Airline cancelled the flight"))

        assertEquals(201, allowed.response.status)
        assertEquals("COMPLETED", statusOfRefund(allowed))
    }
}
