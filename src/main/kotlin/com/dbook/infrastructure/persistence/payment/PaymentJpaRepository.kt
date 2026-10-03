package com.dbook.infrastructure.persistence.payment

import org.springframework.data.jpa.repository.JpaRepository

interface PaymentJpaRepository : JpaRepository<PaymentJpaEntity, Long> {
    fun findByCustomerIdAndIdempotencyKey(
        customerId: Long,
        idempotencyKey: String,
    ): PaymentJpaEntity?
}
