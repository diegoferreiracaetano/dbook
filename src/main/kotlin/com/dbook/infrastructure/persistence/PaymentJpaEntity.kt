package com.dbook.infrastructure.persistence

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

// created_at has a DB-side default (see V20) and isn't needed by any consumer yet, so
// it's deliberately left unmapped here rather than round-tripped through the domain.
@Entity
@Table(name = "payment")
class PaymentJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var customerId: Long = 0,
    var amount: BigDecimal = BigDecimal.ZERO,
    var cardLast4: String = "",
    var cardholderName: String = "",
)
