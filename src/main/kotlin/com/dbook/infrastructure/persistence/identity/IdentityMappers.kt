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
    )

fun User.toJpaEntity(): UserJpaEntity =
    UserJpaEntity(
        id = id,
        email = email,
        passwordHash = passwordHash,
        name = name,
        role = role,
    )

fun RefreshTokenJpaEntity.toDomain(): RefreshToken =
    RefreshToken(
        id = id,
        userId = userId,
        tokenHash = tokenHash,
        expiresAt = expiresAt,
        revoked = revoked,
    )

fun RefreshToken.toJpaEntity(): RefreshTokenJpaEntity =
    RefreshTokenJpaEntity(
        id = id,
        userId = userId,
        tokenHash = tokenHash,
        expiresAt = expiresAt,
        revoked = revoked,
    )
