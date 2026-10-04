package com.dbook.presentation.audit.auditcursorcodec

import com.dbook.domain.audit.AuditCursor
import com.dbook.presentation.audit.AuditCursorCodec
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ACursorSurvivesAnEncodeAndDecodeRoundTripTest {
    @Test
    fun `given a cursor when encoded and decoded then it is the same cursor`() {
        val cursor = AuditCursor(Instant.parse("2026-10-04T12:00:00.123456Z"), 42)

        assertEquals(cursor, AuditCursorCodec.decode(AuditCursorCodec.encode(cursor)))
    }
}
