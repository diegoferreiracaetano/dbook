package com.dbook.presentation.adminbookings

import kotlin.test.Test
import kotlin.test.assertEquals

class ARefundedSeatCanBeBookedAgainTest : AdminBookingsFixture() {
    @Test
    fun `given a refunded booking when another customer books the same seat then it is possible`() {
        val (_, booking) = paidBooking()
        val (bookableId, seatId) =
            jdbcTemplate.queryForMap("SELECT bookable_id, seat_id FROM booking WHERE id = ?", booking)
                .let { it["bookable_id"] as Long to it["seat_id"] as Long }
        refund(staff(), booking)

        val another = registerAndLogin(uniqueEmail())

        assertEquals(true, book(another, bookableId, seatId) > 0)
    }
}
