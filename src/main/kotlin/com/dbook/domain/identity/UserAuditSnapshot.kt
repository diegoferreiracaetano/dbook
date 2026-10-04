package com.dbook.domain.identity

fun User.toAuditSnapshot(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "role" to role.name,
        "status" to status.name,
    )
