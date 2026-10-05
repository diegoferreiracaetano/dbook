package com.dbook.application.crm.addcustomernoteusecase

import com.dbook.application.crm.AddCustomerNoteCommand
import com.dbook.application.crm.CrmUseCaseFixture
import com.dbook.domain.audit.AuditAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AddsANoteAndTheAuditNeverHoldsItsTextTest : CrmUseCaseFixture() {
    @Test
    fun `given a support member when adding a note then it is stored and the audit has no text`() {
        val note = addNote.execute(AddCustomerNoteCommand(support, customerId, "  secret detail  ", pinned = true))

        assertEquals("secret detail", note.body)
        assertEquals(2L, note.authorId)
        val event = audit.events.single()
        assertEquals(AuditAction.CUSTOMER_NOTE_ADDED, event.action)
        assertEquals(note.id.toString(), event.targetId)
        assertFalse(event.after.toString().contains("secret"))
    }
}
