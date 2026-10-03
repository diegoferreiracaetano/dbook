package com.dbook.infrastructure.persistence

import com.dbook.domain.Payment
import com.dbook.domain.PaymentRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository

@Repository
class PaymentRepositoryAdapter(
    private val paymentJpaRepository: PaymentJpaRepository,
) : PaymentRepository {
    override fun save(payment: Payment): Payment =
        try {
            paymentJpaRepository.save(payment.toJpaEntity()).toDomain()
        } catch (ex: DataIntegrityViolationException) {
            throw IllegalStateException("A payment with this Idempotency-Key is already being processed", ex)
        }

    override fun findByCustomerIdAndIdempotencyKey(
        customerId: Long,
        idempotencyKey: String,
    ): Payment? = paymentJpaRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey)?.toDomain()
}
