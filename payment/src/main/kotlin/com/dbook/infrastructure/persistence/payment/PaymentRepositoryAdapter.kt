package com.dbook.infrastructure.persistence.payment

import com.dbook.domain.payment.DuplicateIdempotencyKeyException
import com.dbook.domain.payment.Payment
import com.dbook.domain.payment.PaymentRepository
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
            throw DuplicateIdempotencyKeyException(ex)
        }

    override fun findByCustomerIdAndIdempotencyKey(
        customerId: Long,
        idempotencyKey: String,
    ): Payment? = paymentJpaRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey)?.toDomain()
}
