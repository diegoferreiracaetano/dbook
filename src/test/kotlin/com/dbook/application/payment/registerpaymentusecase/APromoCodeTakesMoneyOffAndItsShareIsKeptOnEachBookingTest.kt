package com.dbook.application.payment.registerpaymentusecase

import com.dbook.application.payment.RegisterPaymentCommand
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class APromoCodeTakesMoneyOffAndItsShareIsKeptOnEachBookingTest : RegisterPaymentUseCaseFixture() {
    @Test
    fun `given a round trip paid with a 10 percent code then the payment, the bookings and the use agree`() {
        val payment =
            executeCommitted(
                RegisterPaymentCommand(
                    bookingIds = listOf(outboundBookingId, returnBookingId),
                    cardLast4 = "4242",
                    cardholderName = "Jane Doe",
                    requestingUserId = ownerId,
                    idempotencyKey = "key-1",
                    promoCode = "welcome10",
                ),
            )

        assertEquals(BigDecimal("800.00"), payment.subtotal)
        assertEquals(BigDecimal("80.00"), payment.discount)
        assertEquals(BigDecimal("720.00"), payment.amount)
        assertEquals("WELCOME10", payment.promoCode)
        assertEquals(BigDecimal("50.00"), bookingRepository.findById(outboundBookingId)!!.discount)
        assertEquals(BigDecimal("450.00"), bookingRepository.findById(outboundBookingId)!!.paidAmount)
        assertEquals(BigDecimal("30.00"), bookingRepository.findById(returnBookingId)!!.discount)
        assertEquals(BigDecimal("500.00"), bookingRepository.findById(outboundBookingId)!!.price)
        assertEquals(listOf(Triple(1L, ownerId, payment.id!!)), promos.redemptions)
    }

    @Test
    fun `given no code when paying then nothing is discounted and nothing is used`() {
        val payment =
            executeCommitted(
                RegisterPaymentCommand(
                    bookingIds = listOf(outboundBookingId, returnBookingId),
                    cardLast4 = "4242",
                    cardholderName = "Jane Doe",
                    requestingUserId = ownerId,
                    idempotencyKey = "key-1",
                    promoCode = null,
                ),
            )

        assertEquals(BigDecimal("0.00"), payment.discount)
        assertEquals(payment.subtotal, payment.amount)
        assertEquals(emptyList(), promos.redemptions)
    }

    @Test
    fun `given a code that tries to take everything when paying then a cent is left on each booking`() {
        val payment =
            executeCommitted(
                RegisterPaymentCommand(
                    bookingIds = listOf(outboundBookingId, returnBookingId),
                    cardLast4 = "4242",
                    cardholderName = "Jane Doe",
                    requestingUserId = ownerId,
                    idempotencyKey = "key-1",
                    promoCode = "FREEBIE",
                ),
            )

        assertEquals(BigDecimal("0.02"), payment.amount)
        assertEquals(BigDecimal("0.01"), bookingRepository.findById(outboundBookingId)!!.paidAmount)
        assertEquals(BigDecimal("0.01"), bookingRepository.findById(returnBookingId)!!.paidAmount)
    }
}
