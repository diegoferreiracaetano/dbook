package com.dbook.application.crm.deletecustomernoteusecase

import com.dbook.application.crm.CrmUseCaseFixture
import com.dbook.application.crm.DeleteCustomerNoteCommand
import com.dbook.application.crm.EditCustomerNoteCommand
import com.dbook.domain.crm.CustomerNoteNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class ADeletedNoteIsGoneForEverythingTest : CrmUseCaseFixture() {
    @Test
    fun `given a deleted note when editing or deleting it again then it is not found, but the row is kept`() {
        val id = requireNotNull(noteBy(support).id)
        deleteNote.execute(DeleteCustomerNoteCommand(support, customerId, id))

        assertFailsWith<CustomerNoteNotFoundException> {
            editNote.execute(EditCustomerNoteCommand(support, customerId, id, "x", null))
        }
        assertFailsWith<CustomerNoteNotFoundException> {
            deleteNote.execute(DeleteCustomerNoteCommand(support, customerId, id))
        }
        assertNotNull(notes.byId(id))
    }
}
