package com.dbook.domain

/** Persistence port for [Payment]. */
interface PaymentRepository {
    fun save(payment: Payment): Payment
}
