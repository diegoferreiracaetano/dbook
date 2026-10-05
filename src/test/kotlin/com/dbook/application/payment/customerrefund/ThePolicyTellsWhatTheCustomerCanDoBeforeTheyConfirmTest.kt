package com.dbook.application.payment.customerrefund

import com.dbook.application.payment.CancellationAction
import com.dbook.application.payment.CancellationBlocker
import com.dbook.domain.booking.BookingNotFoundException
import com.dbook.domain.booking.NotBookingOwnerException
import com.dbook.domain.payment.Refund
import com.dbook.domain.payment.RefundReason
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ThePolicyTellsWhatTheCustomerCanDoBeforeTheyConfirmTest : OwnRefundFixture() {
    @Test
    fun `given a paid booking far from departure when asked then a refund can be requested until a day before`() {
        val result = policy.execute(owner, 100)

        assertEquals(CancellationAction.REFUND_REQUEST, result.action)
        assertEquals(BigDecimal("500.00"), result.refundAmount)
        assertEquals(LocalDateTime.of(2026, 10, 19, 8, 0), result.refundableUntil)
        assertNull(result.blockedBy)
    }

    @Test
    fun `given a booking paid with a discount when asked then the refundable amount is what was really paid`() {
        assertEquals(BigDecimal("460.00"), policy.execute(owner, 103).refundAmount)
    }

    @Test
    fun `given a booking inside the last day when asked then the window is closed, and the amount still shows`() {
        val result = policy.execute(owner, 101)

        assertEquals(CancellationAction.NONE, result.action)
        assertEquals(CancellationBlocker.WINDOW_CLOSED, result.blockedBy)
        assertEquals(BigDecimal("500.00"), result.refundAmount)
    }

    @Test
    fun `given an unpaid booking when asked then it can simply be cancelled and nothing is refundable`() {
        val result = policy.execute(owner, 102)

        assertEquals(CancellationAction.CANCEL, result.action)
        assertNull(result.refundAmount)
    }

    @Test
    fun `given a booking already refunded or with a refund in progress when asked then it says so`() {
        committed { requestOwnRefund.execute(owner, 100, "key-1") }
        refunds.save(
            Refund(
                paymentId = 7,
                bookingId = 103,
                amount = BigDecimal("460.00"),
                reason = RefundReason.CUSTOMER_REQUEST,
                idempotencyKey = "k",
                requestFingerprint = "f",
                requestedBy = 2,
                createdAt = now,
            ),
        )

        assertEquals(CancellationBlocker.ALREADY_REFUNDED, policy.execute(owner, 100).blockedBy)
        assertEquals(CancellationBlocker.REFUND_IN_PROGRESS, policy.execute(owner, 103).blockedBy)
    }

    @Test
    fun `given someone else's booking or none when asked then it is refused`() {
        assertFailsWith<NotBookingOwnerException> { policy.execute(stranger, 100) }
        assertFailsWith<BookingNotFoundException> { policy.execute(owner, 999) }
    }
}
