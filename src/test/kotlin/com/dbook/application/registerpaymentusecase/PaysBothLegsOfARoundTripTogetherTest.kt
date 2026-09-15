package com.dbook.application.registerpaymentusecase

import com.dbook.application.RegisterPaymentCommand
import com.dbook.domain.BookingStatus
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class PaysBothLegsOfARoundTripTogetherTest : RegisterPaymentUseCaseFixture() {
    @Test
    fun `given two PENDING bookings when paid together then the amount sums both legs and both are CONFIRMED`() {
        val payment =
            useCase.execute(
                RegisterPaymentCommand(
                    bookingIds = listOf(outboundBookingId, returnBookingId),
                    cardLast4 = "4242",
                    cardholderName = "Jane Doe",
                    requestingUserId = ownerId,
                ),
            )

        assertEquals(BigDecimal("800.00"), payment.amount)
        assertEquals(BookingStatus.CONFIRMED, bookingRepository.findById(outboundBookingId)!!.status)
        assertEquals(BookingStatus.CONFIRMED, bookingRepository.findById(returnBookingId)!!.status)
    }
}
