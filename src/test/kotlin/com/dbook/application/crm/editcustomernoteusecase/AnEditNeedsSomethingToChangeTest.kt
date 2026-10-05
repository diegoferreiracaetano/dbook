package com.dbook.application.crm.editcustomernoteusecase

import com.dbook.application.crm.CrmUseCaseFixture
import com.dbook.application.crm.EditCustomerNoteCommand
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AnEditNeedsSomethingToChangeTest : CrmUseCaseFixture() {
    @Test
    fun `given neither a text nor a pin when editing then it is refused`() {
        val note = noteBy(support)

        assertFailsWith<IllegalArgumentException> {
            editNote.execute(EditCustomerNoteCommand(support, customerId, requireNotNull(note.id), null, null))
        }
    }
}
