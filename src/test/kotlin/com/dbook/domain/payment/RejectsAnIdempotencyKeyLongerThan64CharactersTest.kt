package com.dbook.domain.payment

import com.dbook.domain.Payment
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsAnIdempotencyKeyLongerThan64CharactersTest {
    @Test
    fun `given an idempotencyKey over 64 characters when a Payment is built then it throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            Payment(
                customerId = 1,
                amount = BigDecimal("100.00"),
                cardLast4 = "4242",
                cardholderName = "Jane Doe",
                idempotencyKey = "k".repeat(65),
            )
        }
    }
}
