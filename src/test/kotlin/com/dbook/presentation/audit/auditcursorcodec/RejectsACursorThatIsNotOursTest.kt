package com.dbook.presentation.audit.auditcursorcodec

import com.dbook.presentation.audit.AuditCursorCodec
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsACursorThatIsNotOursTest {
    @Test
    fun `given garbage, a wrong shape or a bad value when decoding then each is an IllegalArgumentException`() {
        val notAnInstant = Base64.getUrlEncoder().encodeToString("yesterday|1".toByteArray())
        val missingTheId = Base64.getUrlEncoder().encodeToString("2026-10-04T12:00:00Z".toByteArray())

        listOf("%%%", "", notAnInstant, missingTheId).forEach { value ->
            assertFailsWith<IllegalArgumentException> { AuditCursorCodec.decode(value) }
        }
    }
}
