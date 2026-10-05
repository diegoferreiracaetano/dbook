package com.dbook.domain.payment.refund

import com.dbook.domain.payment.Refund
import com.dbook.domain.payment.RefundReason
import com.dbook.domain.payment.RefundStatus
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TheRefundMovesOnlyThroughItsStatesTest {
    private val now = Instant.parse("2026-10-04T12:00:00Z")

    private fun requested() =
        Refund(
            id = 1, paymentId = 7, bookingId = 100, amount = BigDecimal("500.00"), reason = RefundReason.OTHER,
            idempotencyKey = "k", requestFingerprint = "f", requestedBy = 2, createdAt = now,
        )

    @Test
    fun `given a requested refund when it completes then it has the moment, and nothing else moves it`() {
        val done = requested().complete(now)

        assertEquals(RefundStatus.COMPLETED, done.status)
        assertEquals(now, done.completedAt)
        assertFailsWith<IllegalStateException> { done.complete(now) }
        assertFailsWith<IllegalStateException> { done.fail("late") }
        assertFailsWith<IllegalStateException> { done.reopen() }
    }

    @Test
    fun `given a requested refund when it fails then the reason is kept, cut to 255, and it can be reopened`() {
        val failed = requested().fail("x".repeat(300))

        assertEquals(RefundStatus.FAILED, failed.status)
        assertEquals(255, failed.failureReason?.length)
        val reopened = failed.reopen()
        assertEquals(RefundStatus.REQUESTED, reopened.status)
        assertNull(reopened.failureReason)
        assertFailsWith<IllegalStateException> { requested().reopen() }
    }

    @Test
    fun `given a zero amount, a blank key or a long note when building then it is refused`() {
        assertFailsWith<IllegalArgumentException> {
            Refund(
                paymentId = 1,
                bookingId = 1,
                amount = BigDecimal.ZERO,
                reason = RefundReason.OTHER,
                idempotencyKey = "k",
                requestFingerprint = "f",
                requestedBy = 1,
                createdAt = now,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            Refund(
                paymentId = 1,
                bookingId = 1,
                amount = BigDecimal.ONE,
                reason = RefundReason.OTHER,
                idempotencyKey = " ",
                requestFingerprint = "f",
                requestedBy = 1,
                createdAt = now,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            Refund(
                paymentId = 1, bookingId = 1, amount = BigDecimal.ONE, reason = RefundReason.OTHER,
                note =
                    "n".repeat(
                        501,
                    ),
                idempotencyKey = "k", requestFingerprint = "f", requestedBy = 1, createdAt = now,
            )
        }
    }
}
