package com.dbook.infrastructure.persistence.payment

import com.dbook.domain.payment.Payment

fun PaymentJpaEntity.toDomain(): Payment =
    Payment(
        id = id,
        customerId = customerId,
        amount = amount,
        cardLast4 = cardLast4,
        cardholderName = cardholderName,
        idempotencyKey = idempotencyKey,
        requestFingerprint = requestFingerprint,
    )

fun Payment.toJpaEntity(): PaymentJpaEntity =
    PaymentJpaEntity(
        id = id,
        customerId = customerId,
        amount = amount,
        cardLast4 = cardLast4,
        cardholderName = cardholderName,
        idempotencyKey = idempotencyKey,
        requestFingerprint = requestFingerprint,
    )
