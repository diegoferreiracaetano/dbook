package com.dbook.presentation.payment

import com.dbook.domain.payment.Payment
import java.math.BigDecimal

data class PaymentResponse(
    val id: Long?,
    val amount: BigDecimal,
    val subtotal: BigDecimal,
    val discount: BigDecimal,
    val promoCode: String?,
    val cardLast4: String,
    val bookingIds: List<Long>,
    val status: String,
) {
    companion object {
        fun from(
            payment: Payment,
            bookingIds: List<Long>,
        ) = PaymentResponse(
            id = payment.id,
            amount = payment.amount,
            subtotal = payment.subtotal,
            discount = payment.discount,
            promoCode = payment.promoCode,
            cardLast4 = payment.cardLast4,
            bookingIds = bookingIds,
            status = "CONFIRMED",
        )
    }
}
