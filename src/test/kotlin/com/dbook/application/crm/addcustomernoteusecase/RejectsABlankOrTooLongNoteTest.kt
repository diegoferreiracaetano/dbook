package com.dbook.application.crm.addcustomernoteusecase

import com.dbook.application.crm.AddCustomerNoteCommand
import com.dbook.application.crm.CrmUseCaseFixture
import com.dbook.domain.crm.CustomerNote
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsABlankOrTooLongNoteTest : CrmUseCaseFixture() {
    @Test
    fun `given a blank text or one over 2000 characters when adding a note then it is refused`() {
        assertFailsWith<IllegalArgumentException> {
            addNote.execute(AddCustomerNoteCommand(support, customerId, "   ", pinned = false))
        }
        assertFailsWith<IllegalArgumentException> {
            addNote.execute(
                AddCustomerNoteCommand(support, customerId, "a".repeat(CustomerNote.MAX_LENGTH + 1), pinned = false),
            )
        }
    }
}
