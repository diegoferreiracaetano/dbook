package com.dbook.domain

/** Persistence port for [Payment]. */
interface PaymentRepository {
    fun save(payment: Payment): Payment

    /** @return the payment this customer already made with [idempotencyKey], if any. */
    fun findByCustomerIdAndIdempotencyKey(
        customerId: Long,
        idempotencyKey: String,
    ): Payment?
}
