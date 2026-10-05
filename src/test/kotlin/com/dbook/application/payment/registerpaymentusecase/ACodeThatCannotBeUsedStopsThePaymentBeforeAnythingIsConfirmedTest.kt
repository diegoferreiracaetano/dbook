package com.dbook.application.payment.registerpaymentusecase

import com.dbook.application.payment.RegisterPaymentCommand
import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.promo.PromoNotFoundException
import com.dbook.domain.promo.PromoRejectedException
import com.dbook.domain.promo.PromoRejection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ACodeThatCannotBeUsedStopsThePaymentBeforeAnythingIsConfirmedTest : RegisterPaymentUseCaseFixture() {
    private fun command(
        code: String,
        bookings: List<Long> = listOf(outboundBookingId, returnBookingId),
        key: String = "key-1",
    ) = RegisterPaymentCommand(
        bookingIds = bookings,
        cardLast4 = "4242",
        cardholderName = "Jane Doe",
        requestingUserId = ownerId,
        idempotencyKey = key,
        promoCode = code,
    )

    private fun rejection(code: String): PromoRejection =
        assertFailsWith<PromoRejectedException> { executeCommitted(command(code)) }.reason

    @Test
    fun `given a code that cannot be used when paying then it is rejected and both bookings stay PENDING`() {
        assertEquals(PromoRejection.EXPIRED, rejection("EXPIRED"))
        assertEquals(PromoRejection.BELOW_MINIMUM, rejection("BIG"))

        assertEquals(BookingStatus.PENDING, bookingRepository.findById(outboundBookingId)!!.status)
        assertEquals(BookingStatus.PENDING, bookingRepository.findById(returnBookingId)!!.status)
        assertEquals(0, paymentRepository.saved.size)
    }

    @Test
    fun `given a code that does not exist when paying then it is not found`() {
        assertFailsWith<PromoNotFoundException> { executeCommitted(command("NOPE")) }
    }

    @Test
    fun `given a code with one use left taken by someone else when paying then it is exhausted`() {
        promos.redeem(3, 99, 1, java.math.BigDecimal("5.00"), now)

        assertEquals(PromoRejection.EXHAUSTED, rejection("ONCE"))
        assertEquals(BookingStatus.PENDING, bookingRepository.findById(outboundBookingId)!!.status)
    }

    @Test
    fun `given a customer who already used a once-per-customer code when paying again then the limit is reached`() {
        executeCommitted(command("WELCOME10", listOf(outboundBookingId), "key-1"))

        val reason =
            assertFailsWith<PromoRejectedException> {
                executeCommitted(command("WELCOME10", listOf(returnBookingId), "key-2"))
            }.reason

        assertEquals(PromoRejection.USER_LIMIT_REACHED, reason)
        assertEquals(BookingStatus.PENDING, bookingRepository.findById(returnBookingId)!!.status)
    }
}
