package com.dbook.presentation.adminbookings

import com.dbook.application.booking.ExpireBookingUseCase
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnExpiredBookingShowsTheSystemAsTheActorTest : AdminBookingsFixture() {
    @Autowired
    lateinit var expireBookingUseCase: ExpireBookingUseCase

    @Test
    fun `given a pending booking that expires when reading its timeline then the cancellation has no actor`() {
        val customer = registerAndLogin(uniqueEmail())
        val (booking) = bookSeats(customer, 1)

        expireBookingUseCase.execute(booking)

        val timeline = bodyOf(adminBooking(staff(), booking))["timeline"]
        assertEquals(listOf("PENDING", "CANCELLED"), timeline.map { it["to"].asText() })
        assertTrue(timeline[1]["actorId"].isNull)
    }
}
