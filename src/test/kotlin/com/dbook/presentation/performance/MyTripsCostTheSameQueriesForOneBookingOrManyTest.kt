package com.dbook.presentation.performance

import com.dbook.QueryCounter
import com.dbook.presentation.adminbookings.AdminBookingsFixture
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test
import kotlin.test.assertEquals

// A list that runs one more query for each row (the N+1) is invisible with three rows in a test and slow with three
// thousand in production. These tests fail the day a list starts to ask something per row.
class MyTripsCostTheSameQueriesForOneBookingOrManyTest : AdminBookingsFixture() {
    @Autowired
    lateinit var queryCounter: QueryCounter

    @Test
    fun `given a customer when the bookings grow from one to five then the list costs the same queries`() {
        val token = registerAndLogin(uniqueEmail())
        bookSeats(token, 1)
        myBookings(token)

        val withOne = queryCounter.queriesOf { myBookings(token) }
        bookSeats(token, 4)
        val withFive = queryCounter.queriesOf { myBookings(token) }

        assertEquals(withOne, withFive, "the list asks something more for each booking")
    }
}
