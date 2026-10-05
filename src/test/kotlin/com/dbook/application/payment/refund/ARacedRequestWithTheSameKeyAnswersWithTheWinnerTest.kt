package com.dbook.application.payment.refund

import com.dbook.domain.payment.Refund
import com.dbook.domain.payment.RefundReason
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class ARacedRequestWithTheSameKeyAnswersWithTheWinnerTest : RefundUseCaseFixture() {
    @Test
    fun `given the same request sent twice at once when the second loses the insert then it gets the first refund`() {
        // the winner has inserted its REQUESTED refund but the loser's lookup, made just before, did not see it
        val sent = command()
        val winner =
            refunds.save(
                Refund(
                    paymentId = 7,
                    bookingId = 100,
                    amount = BigDecimal("500.00"),
                    reason = RefundReason.CUSTOMER_REQUEST,
                    idempotencyKey = sent.idempotencyKey,
                    requestFingerprint = sent.fingerprint(),
                    requestedBy = sent.actor.id,
                    createdAt = now,
                ),
            )
        refunds.hideKeyLookupOnce = true

        val loser = committed { refundBooking.execute(sent) }

        assertEquals(winner.id, loser.id)
        assertEquals(1, refunds.all().size)
        assertEquals(1.0, counted("replayed"))
    }
}
