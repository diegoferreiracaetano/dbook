package com.dbook.presentation.adminbookings

import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals

class TwoAttendantsRefundingTheSameBookingOnlyOneWinsTest : AdminBookingsFixture() {
    @Test
    fun `given two attendants refunding the same booking at once when both send it then one wins and the other 409`() {
        val (_, booking) = paidBooking()
        val first = staff()
        val second = staff()
        val before = gateway.calls.size
        val barrier = CyclicBarrier(2)
        val pool = Executors.newFixedThreadPool(2)

        val statuses =
            try {
                listOf(first, second).map { token ->
                    pool.submit<Int> {
                        barrier.await()
                        refund(token, booking).response.status
                    }
                }.map { it.get() }
            } finally {
                pool.shutdown()
            }

        assertEquals(listOf(201, 409), statuses.sorted())
        assertEquals(before + 1, gateway.calls.size)
        assertEquals("REFUNDED", bodyOf(adminBooking(first, booking))["booking"]["status"].asText())
    }
}
