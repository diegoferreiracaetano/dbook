package com.dbook.domain.identity

fun StaffInvitation.toAuditSnapshot(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "role" to role.name,
        "invitedBy" to invitedBy,
        "expiresAt" to expiresAt.toString(),
        "acceptedAt" to acceptedAt?.toString(),
        "revokedAt" to revokedAt?.toString(),
    )
