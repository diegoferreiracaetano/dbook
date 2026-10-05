package com.dbook.application.crm.deletecustomernoteusecase

import com.dbook.application.crm.CrmUseCaseFixture
import com.dbook.application.crm.DeleteCustomerNoteCommand
import com.dbook.domain.crm.NoteAccessDeniedException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class AnotherSupportMemberCannotDeleteANoteTest : CrmUseCaseFixture() {
    @Test
    fun `given a note of a colleague when a support member deletes it then it is refused and it stays`() {
        val id = requireNotNull(noteBy(support).id)

        assertFailsWith<NoteAccessDeniedException> {
            deleteNote.execute(DeleteCustomerNoteCommand(otherSupport, customerId, id))
        }

        assertNotNull(notes.findActive(customerId, id))
    }
}
