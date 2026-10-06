package com.dbook.infrastructure.persistence.payment

import com.dbook.domain.payment.RefundStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface RefundJpaRepository : JpaRepository<RefundJpaEntity, Long> {
    fun findByRequestedByAndIdempotencyKey(
        requestedBy: Long,
        idempotencyKey: String,
    ): RefundJpaEntity?

    fun findByBookingIdAndStatus(
        bookingId: Long,
        status: RefundStatus,
    ): RefundJpaEntity?

    fun findByStatus(
        status: RefundStatus,
        pageable: Pageable,
    ): Page<RefundJpaEntity>
}
