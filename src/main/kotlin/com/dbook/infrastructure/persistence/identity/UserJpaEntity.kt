package com.dbook.infrastructure.persistence.identity

import com.dbook.domain.identity.Role
import com.dbook.domain.identity.UserStatus
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
@Table(name = "app_user")
class UserJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var email: String = "",
    var passwordHash: String = "",
    var name: String = "",
    @Enumerated(EnumType.STRING)
    var role: Role = Role.CLIENT,
    @Enumerated(EnumType.STRING)
    var status: UserStatus = UserStatus.ACTIVE,
    var blockedReason: String? = null,
    var blockedAt: Instant? = null,
    var lastLoginAt: Instant? = null,
    var anonymizedAt: Instant? = null,
    var emailVerifiedAt: Instant? = null,
    @Version
    var version: Long = 0,
)
