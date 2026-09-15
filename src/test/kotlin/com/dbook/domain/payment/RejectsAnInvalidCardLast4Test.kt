package com.dbook.domain.payment

import com.dbook.domain.Payment
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsAnInvalidCardLast4Test {
    @Test
    fun `given a cardLast4 that isn't 4 digits when a Payment is built then it throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            Payment(customerId = 1, amount = BigDecimal("100.00"), cardLast4 = "42", cardholderName = "Jane Doe")
        }
    }
}
