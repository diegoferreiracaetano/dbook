package com.dbook.infrastructure.persistence.payment

import com.dbook.domain.payment.RefundReason
import com.dbook.domain.payment.RefundStatus
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "refund")
class RefundJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var paymentId: Long = 0,
    var bookingId: Long = 0,
    var amount: BigDecimal = BigDecimal.ZERO,
    @Enumerated(EnumType.STRING)
    var reason: RefundReason = RefundReason.OTHER,
    var note: String? = null,
    @Enumerated(EnumType.STRING)
    var status: RefundStatus = RefundStatus.REQUESTED,
    var idempotencyKey: String = "",
    var requestFingerprint: String = "",
    var requestedBy: Long = 0,
    var failureReason: String? = null,
    var createdAt: Instant = Instant.EPOCH,
    var completedAt: Instant? = null,
    @Version
    var version: Long = 0,
)
