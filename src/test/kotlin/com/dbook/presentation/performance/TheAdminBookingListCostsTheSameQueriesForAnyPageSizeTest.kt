package com.dbook.presentation.performance

import com.dbook.QueryCounter
import com.dbook.presentation.adminbookings.AdminBookingsFixture
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test
import kotlin.test.assertEquals

class TheAdminBookingListCostsTheSameQueriesForAnyPageSizeTest : AdminBookingsFixture() {
    @Autowired
    lateinit var queryCounter: QueryCounter

    @Test
    fun `given enough bookings when a page of one and a page of five are listed then the cost is the same`() {
        val token = registerAndLogin(uniqueEmail())
        bookSeats(token, 5)
        val team = staff()
        searchBookings(team, "size" to "1")

        val one = queryCounter.queriesOf { searchBookings(team, "size" to "1") }
        val five = queryCounter.queriesOf { searchBookings(team, "size" to "5") }

        assertEquals(one, five, "the list asks something more for each row")
    }
}
