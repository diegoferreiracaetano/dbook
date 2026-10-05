package com.dbook.application.crm.editcustomernoteusecase

import com.dbook.application.crm.CrmUseCaseFixture
import com.dbook.application.crm.EditCustomerNoteCommand
import com.dbook.domain.crm.CustomerNoteNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ANoteOfAnotherCustomerIsNotFoundTest : CrmUseCaseFixture() {
    @Test
    fun `given a note of customer 3 when editing it through customer 99 then it is not found`() {
        val note = noteBy(support)

        assertFailsWith<CustomerNoteNotFoundException> {
            editNote.execute(EditCustomerNoteCommand(support, 99, requireNotNull(note.id), "x", null))
        }
    }
}
