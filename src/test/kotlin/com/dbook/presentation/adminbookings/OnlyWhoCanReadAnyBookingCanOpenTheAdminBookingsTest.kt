package com.dbook.presentation.adminbookings

import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class OnlyWhoCanReadAnyBookingCanOpenTheAdminBookingsTest : AdminBookingsFixture() {
    @Test
    fun `given a catalog manager, a customer or an unknown id when reading admin bookings then 403, 403 and 404`() {
        val (customer, booking) = paidBooking()

        assertEquals(403, searchBookings(staff(Role.CATALOG_MANAGER)).response.status)
        assertEquals(403, adminBooking(customer, booking).response.status)
        assertEquals(404, adminBooking(staff(), 999_999_999L).response.status)
        assertEquals(200, adminBooking(staff(), booking).response.status)
    }
}
