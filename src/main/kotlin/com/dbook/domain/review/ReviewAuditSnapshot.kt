package com.dbook.domain.review

// What the audit trail keeps of a review: the state, never the text (a comment may carry personal data).
fun Review.toAuditSnapshot(): Map<String, Any?> =
    mapOf("id" to id, "rating" to rating, "status" to status.name, "hiddenReason" to hiddenReason)
