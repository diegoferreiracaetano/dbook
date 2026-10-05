package com.dbook.application.payment.refund

import com.dbook.domain.booking.BookingNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OnlyAConfirmedBookingCanBeRefundedTest : RefundUseCaseFixture() {
    @Test
    fun `given a pending, an already refunded or an unknown booking when refunding then 409, 409 and not found`() {
        assertFailsWith<IllegalStateException> { committed { refundBooking.execute(command(bookingId = 102)) } }
        committed { refundBooking.execute(command(bookingId = 100, key = "k1")) }
        assertFailsWith<IllegalStateException> {
            committed {
                refundBooking.execute(
                    command(bookingId = 100, key = "k2"),
                )
            }
        }
        assertFailsWith<BookingNotFoundException> { committed { refundBooking.execute(command(bookingId = 999)) } }

        assertEquals(1, gateway.calls.size)
    }
}
