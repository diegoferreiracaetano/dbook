package com.dbook.application.payment.registerpaymentusecase

import com.dbook.application.payment.RegisterPaymentCommand
import com.dbook.domain.payment.IdempotencyKeyReusedException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TheCodeIsPartOfTheIdempotentRequestTest : RegisterPaymentUseCaseFixture() {
    private fun command(code: String?) =
        RegisterPaymentCommand(
            bookingIds = listOf(outboundBookingId, returnBookingId),
            cardLast4 = "4242",
            cardholderName = "Jane Doe",
            requestingUserId = ownerId,
            idempotencyKey = "key-1",
            promoCode = code,
        )

    @Test
    fun `given a payment with a code when the request is repeated then the same payment returns, the code used once`() {
        val first = executeCommitted(command("WELCOME10"))

        val second = executeCommitted(command("  welcome10"))

        assertEquals(first.id, second.id)
        assertEquals(1, promos.redemptions.size)
        assertEquals(1, paymentRepository.saved.size)
    }

    @Test
    fun `given a payment with a code when the key comes with another code or none then it is reused`() {
        executeCommitted(command("WELCOME10"))

        assertFailsWith<IdempotencyKeyReusedException> { executeCommitted(command("FIVE")) }
        assertFailsWith<IdempotencyKeyReusedException> { executeCommitted(command(null)) }
        assertEquals(1, promos.redemptions.size)
    }
}
