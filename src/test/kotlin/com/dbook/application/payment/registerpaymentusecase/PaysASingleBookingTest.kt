package com.dbook.application.payment.registerpaymentusecase

import com.dbook.application.payment.RegisterPaymentCommand
import com.dbook.domain.booking.BookingStatus
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class PaysASingleBookingTest : RegisterPaymentUseCaseFixture() {
    @Test
    fun `given one PENDING booking when paid then the payment is saved for its price and the booking is CONFIRMED`() {
        val payment =
            executeCommitted(
                RegisterPaymentCommand(
                    bookingIds = listOf(outboundBookingId),
                    cardLast4 = "4242",
                    cardholderName = "Jane Doe",
                    requestingUserId = ownerId,
                    idempotencyKey = "key-1",
                ),
            )

        assertEquals(BigDecimal("500.00"), payment.amount)
        val confirmed = bookingRepository.findById(outboundBookingId)!!
        assertEquals(BookingStatus.CONFIRMED, confirmed.status)
        assertEquals(payment.id, confirmed.paymentId)
    }
}
