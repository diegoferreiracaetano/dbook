package com.dbook.infrastructure.persistence.identity

import com.dbook.domain.identity.StaffInvitation

fun StaffInvitationJpaEntity.toDomain(): StaffInvitation =
    StaffInvitation(
        id = id,
        email = email,
        role = role,
        tokenHash = tokenHash,
        invitedBy = invitedBy,
        expiresAt = expiresAt,
        createdAt = createdAt,
        acceptedAt = acceptedAt,
        revokedAt = revokedAt,
        version = version,
    )

fun StaffInvitation.toJpaEntity(): StaffInvitationJpaEntity =
    StaffInvitationJpaEntity(
        id = id,
        email = email,
        role = role,
        tokenHash = tokenHash,
        invitedBy = invitedBy,
        expiresAt = expiresAt,
        createdAt = createdAt,
        acceptedAt = acceptedAt,
        revokedAt = revokedAt,
        version = version,
    )
