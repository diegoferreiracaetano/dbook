package com.dbook.application.crm.deletecustomernoteusecase

import com.dbook.application.crm.CrmUseCaseFixture
import com.dbook.application.crm.DeleteCustomerNoteCommand
import com.dbook.domain.audit.AuditAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TheAuthorOrASuperAdminDeletesTest : CrmUseCaseFixture() {
    @Test
    fun `given the author and a SUPER_ADMIN when each deletes a note then both work and the audit has them`() {
        val mine = requireNotNull(noteBy(support).id)
        val theirs = requireNotNull(noteBy(otherSupport).id)

        deleteNote.execute(DeleteCustomerNoteCommand(support, customerId, mine))
        deleteNote.execute(DeleteCustomerNoteCommand(superAdmin, customerId, theirs))

        assertNull(notes.findActive(customerId, mine))
        assertNull(notes.findActive(customerId, theirs))
        assertEquals(2, audit.events.count { it.action == AuditAction.CUSTOMER_NOTE_DELETED })
    }
}
