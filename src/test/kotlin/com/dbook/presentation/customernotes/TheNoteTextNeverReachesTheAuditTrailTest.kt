package com.dbook.presentation.customernotes

import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TheNoteTextNeverReachesTheAuditTrailTest : CustomerNotesFixture() {
    @Test
    fun `given a note written, edited and deleted when reading the audit then three entries and none has the text`() {
        val token = staffToken()
        val customerId = newCustomerId()
        val noteId = noteIdOf(addNote(token, customerId, "confidential-text-123"))
        editNote(token, customerId, noteId, mapOf("body" to "confidential-edit-456"))
        deleteNote(token, customerId, noteId)

        val trail = auditEntries(staffToken(Role.SUPER_ADMIN), "targetType=CUSTOMER_NOTE&targetId=$noteId")

        assertEquals(3, trail.size())
        assertFalse(trail.toString().contains("confidential"))
    }
}
