package com.dbook.infrastructure.persistence.identity

import com.dbook.domain.identity.Role
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant

@Entity
@Table(name = "staff_invitation")
class StaffInvitationJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var email: String = "",
    @Enumerated(EnumType.STRING)
    var role: Role = Role.SUPPORT,
    var tokenHash: String = "",
    var invitedBy: Long = 0,
    var expiresAt: Instant = Instant.EPOCH,
    var createdAt: Instant = Instant.EPOCH,
    var acceptedAt: Instant? = null,
    var revokedAt: Instant? = null,
    @Version
    var version: Long = 0,
)
