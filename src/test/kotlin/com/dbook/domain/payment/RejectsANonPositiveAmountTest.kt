package com.dbook.domain.payment

import com.dbook.domain.Payment
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsANonPositiveAmountTest {
    @Test
    fun `given a zero amount when a Payment is built then it throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            Payment(customerId = 1, amount = BigDecimal.ZERO, cardLast4 = "4242", cardholderName = "Jane Doe")
        }
    }
}
