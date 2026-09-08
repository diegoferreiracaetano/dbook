package com.dbook.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

// user_id is a plain column, not a @ManyToOne: nothing here ever needs to navigate to
// the full User object, just its id. The FK constraint still lives at the DB level
// (see migration V6), just not modeled as a JPA relationship.
@Entity
@Table(name = "refresh_token")
class RefreshTokenJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "user_id")
    var userId: Long = 0,
    @Column(name = "token_hash")
    var tokenHash: String = "",
    @Column(name = "expires_at")
    var expiresAt: Instant = Instant.EPOCH,
    var revoked: Boolean = false,
)
