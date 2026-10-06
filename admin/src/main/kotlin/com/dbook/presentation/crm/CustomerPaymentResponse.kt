package com.dbook.presentation.crm

import com.dbook.domain.crm.CustomerPayment
import java.math.BigDecimal
import java.time.LocalDateTime

data class CustomerPaymentResponse(
    val id: Long,
    val amount: BigDecimal,
    val cardLast4: String,
    val createdAt: LocalDateTime,
    val bookingIds: List<Long>,
) {
    companion object {
        fun from(payment: CustomerPayment) =
            CustomerPaymentResponse(
                id = payment.id,
                amount = payment.amount,
                cardLast4 = payment.cardLast4,
                createdAt = payment.createdAt,
                bookingIds = payment.bookingIds,
            )
    }
}
