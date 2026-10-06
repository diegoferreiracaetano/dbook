package com.dbook.domain.crm

// never the text: a note is free-form personal data, and the trail only needs to say that it changed
fun CustomerNote.toAuditSnapshot(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "customerId" to customerId,
        "pinned" to pinned,
        "deleted" to isDeleted,
    )
