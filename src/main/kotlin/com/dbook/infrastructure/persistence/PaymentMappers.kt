package com.dbook.infrastructure.persistence

import com.dbook.domain.Payment

fun PaymentJpaEntity.toDomain(): Payment =
    Payment(
        id = id,
        customerId = customerId,
        amount = amount,
        cardLast4 = cardLast4,
        cardholderName = cardholderName,
    )

fun Payment.toJpaEntity(): PaymentJpaEntity =
    PaymentJpaEntity(
        id = id,
        customerId = customerId,
        amount = amount,
        cardLast4 = cardLast4,
        cardholderName = cardholderName,
    )
