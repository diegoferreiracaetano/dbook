package com.dbook.presentation.adminbookings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TheTimelineShowsEveryStatusAndWhoDidItTest : AdminBookingsFixture() {
    @Test
    fun `given a booking made, paid and refunded when reading it then the timeline has three steps and actors`() {
        val email = uniqueEmail()
        val customer = registerAndLogin(email)
        val (booking) = bookSeats(customer, 1)
        pay(customer, booking)
        val staffEmail = uniqueEmail()
        val support = registerStaffAndLogin(staffEmail, com.dbook.domain.common.access.Role.SUPPORT)
        refund(support, booking)

        val timeline = bodyOf(adminBooking(support, booking))["timeline"]

        assertEquals(listOf("PENDING", "CONFIRMED", "REFUNDED"), timeline.map { it["to"].asText() })
        assertNull(timeline[0]["from"].takeIf { !it.isNull })
        assertEquals("PENDING", timeline[1]["from"].asText())
        assertEquals(userIdOf(email), timeline[0]["actorId"].asLong())
        assertEquals(userIdOf(email), timeline[1]["actorId"].asLong())
        assertEquals(userIdOf(staffEmail), timeline[2]["actorId"].asLong())
    }
}
