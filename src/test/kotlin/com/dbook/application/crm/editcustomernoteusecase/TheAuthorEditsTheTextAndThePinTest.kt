package com.dbook.application.crm.editcustomernoteusecase

import com.dbook.application.crm.CrmUseCaseFixture
import com.dbook.application.crm.EditCustomerNoteCommand
import com.dbook.domain.audit.AuditAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TheAuthorEditsTheTextAndThePinTest : CrmUseCaseFixture() {
    @Test
    fun `given the author when editing then the text and the pin change and the audit has before and after`() {
        val note = noteBy(support)

        val edited =
            editNote.execute(
                EditCustomerNoteCommand(support, customerId, requireNotNull(note.id), "new", true),
            )

        assertEquals("new", edited.body)
        assertTrue(edited.pinned)
        assertEquals(now, edited.editedAt)
        val event = audit.events.last()
        assertEquals(AuditAction.CUSTOMER_NOTE_EDITED, event.action)
        assertEquals(false, event.before?.get("pinned"))
        assertEquals(true, event.after?.get("pinned"))
    }
}
