package com.dbook.infrastructure.persistence.identity

import com.dbook.domain.identity.RefreshToken
import com.dbook.domain.identity.User

fun UserJpaEntity.toDomain(): User =
    User(
        id = id,
        email = email,
        passwordHash = passwordHash,
        name = name,
        role = role,
        status = status,
        blockedReason = blockedReason,
        blockedAt = blockedAt,
        lastLoginAt = lastLoginAt,
        anonymizedAt = anonymizedAt,
        emailVerifiedAt = emailVerifiedAt,
        version = version,
        createdAt = createdAt,
    )

fun User.toJpaEntity(): UserJpaEntity =
    UserJpaEntity(
        id = id,
        email = email,
        passwordHash = passwordHash,
        name = name,
        role = role,
        status = status,
        blockedReason = blockedReason,
        blockedAt = blockedAt,
        lastLoginAt = lastLoginAt,
        anonymizedAt = anonymizedAt,
        emailVerifiedAt = emailVerifiedAt,
        version = version,
    )

fun RefreshTokenJpaEntity.toDomain(): RefreshToken =
    RefreshToken(
        id = id,
        userId = userId,
        tokenHash = tokenHash,
        expiresAt = expiresAt,
        revoked = revoked,
        familyId = familyId.toString(),
        createdAt = createdAt,
    )

fun RefreshToken.toJpaEntity(): RefreshTokenJpaEntity =
    RefreshTokenJpaEntity(
        id = id,
        userId = userId,
        tokenHash = tokenHash,
        expiresAt = expiresAt,
        revoked = revoked,
        familyId = java.util.UUID.fromString(familyId),
    )
