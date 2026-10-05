package com.dbook.infrastructure.persistence.crm

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant

@Entity
@Table(name = "customer_note")
class CustomerNoteJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var customerId: Long = 0,
    var authorId: Long = 0,
    var body: String = "",
    var pinned: Boolean = false,
    var createdAt: Instant = Instant.EPOCH,
    var editedAt: Instant? = null,
    var deletedAt: Instant? = null,
    @Version
    var version: Long = 0,
)
