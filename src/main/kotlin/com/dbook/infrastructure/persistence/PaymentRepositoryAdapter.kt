package com.dbook.infrastructure.persistence

import com.dbook.domain.Payment
import com.dbook.domain.PaymentRepository
import org.springframework.stereotype.Repository

@Repository
class PaymentRepositoryAdapter(
    private val paymentJpaRepository: PaymentJpaRepository,
) : PaymentRepository {
    override fun save(payment: Payment): Payment = paymentJpaRepository.save(payment.toJpaEntity()).toDomain()
}
