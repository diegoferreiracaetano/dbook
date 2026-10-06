package com.dbook.infrastructure.persistence.payment

import com.dbook.domain.payment.Refund

fun RefundJpaEntity.toDomain() =
    Refund(
        id, paymentId, bookingId, amount, reason, note, status, idempotencyKey, requestFingerprint, requestedBy,
        failureReason, createdAt, completedAt, version,
    )

fun Refund.toJpaEntity() =
    RefundJpaEntity(
        id, paymentId, bookingId, amount, reason, note, status, idempotencyKey, requestFingerprint, requestedBy,
        failureReason, createdAt, completedAt, version,
    )
