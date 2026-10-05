package com.dbook.domain.booking.booking

import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.catalog.Bookable
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OnlyAConfirmedBookingCanBeRefundedTest {
    private val bookable =
        object : Bookable(1, "Flight", BigDecimal("100.00"), totalCapacity = 1, availableCapacity = 0) {}

    @Test
    fun `given a confirmed booking when refunded then REFUNDED keeps the price, and it is terminal`() {
        val refunded = Booking(1, bookable, 10, 3).confirm(paymentId = 7).refund()

        assertEquals(BookingStatus.REFUNDED, refunded.status)
        assertEquals(BigDecimal("100.00"), refunded.price)
        assertEquals(7L, refunded.paymentId)
        assertFailsWith<IllegalStateException> { refunded.refund() }
        assertFailsWith<IllegalStateException> { refunded.cancel() }
        assertFailsWith<IllegalStateException> { refunded.confirm(8) }
    }

    @Test
    fun `given a pending or a cancelled booking when refunded then it is refused`() {
        assertFailsWith<IllegalStateException> { Booking(1, bookable, 10, 3).refund() }
        assertFailsWith<IllegalStateException> { Booking(1, bookable, 10, 3).cancel().refund() }
    }
}
