package com.dbook.presentation.audit

import com.dbook.domain.audit.AuditCursor
import java.time.Instant
import java.util.Base64

// The cursor is opaque to clients: they hand back whatever they were given. Anything else is a 400.
object AuditCursorCodec {
    private const val SEPARATOR = "|"

    fun encode(cursor: AuditCursor): String =
        Base64.getUrlEncoder().withoutPadding()
            .encodeToString("${cursor.occurredAt}$SEPARATOR${cursor.id}".toByteArray())

    fun decode(value: String): AuditCursor =
        runCatching {
            val (occurredAt, id) = String(Base64.getUrlDecoder().decode(value)).split(SEPARATOR)
            AuditCursor(Instant.parse(occurredAt), id.toLong())
        }.getOrElse { throw IllegalArgumentException("cursor is not valid") }
}
