package com.dbook.application.crm.editcustomernoteusecase

import com.dbook.application.crm.CrmUseCaseFixture
import com.dbook.application.crm.EditCustomerNoteCommand
import com.dbook.domain.crm.NoteAccessDeniedException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OnlyTheAuthorEditsANoteTest : CrmUseCaseFixture() {
    @Test
    fun `given another support member when editing then it is refused, even a SUPER_ADMIN, and nothing changes`() {
        val note = noteBy(support)
        val id = requireNotNull(note.id)

        assertFailsWith<NoteAccessDeniedException> {
            editNote.execute(EditCustomerNoteCommand(otherSupport, customerId, id, "hijack", null))
        }
        assertFailsWith<NoteAccessDeniedException> {
            editNote.execute(EditCustomerNoteCommand(superAdmin, customerId, id, "hijack", null))
        }

        assertEquals("called about a refund", notes.byId(id)?.body)
    }
}
