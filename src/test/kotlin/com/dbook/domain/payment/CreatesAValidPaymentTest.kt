package com.dbook.domain.payment

import com.dbook.domain.Payment
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class CreatesAValidPaymentTest {
    @Test
    fun `given valid data when a Payment is built then its fields are set`() {
        val payment =
            Payment(
                customerId = 1,
                amount = BigDecimal("585.00"),
                cardLast4 = "4242",
                cardholderName = "Jane Doe",
            )

        assertEquals(BigDecimal("585.00"), payment.amount)
        assertEquals("4242", payment.cardLast4)
        assertEquals("Jane Doe", payment.cardholderName)
    }
}
