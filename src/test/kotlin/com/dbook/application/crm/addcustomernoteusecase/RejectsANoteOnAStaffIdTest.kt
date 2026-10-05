package com.dbook.application.crm.addcustomernoteusecase

import com.dbook.application.crm.AddCustomerNoteCommand
import com.dbook.application.crm.CrmUseCaseFixture
import com.dbook.domain.identity.UserNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsANoteOnAStaffIdTest : CrmUseCaseFixture() {
    @Test
    fun `given the id of a staff member when adding a note then it is not found`() {
        assertFailsWith<UserNotFoundException> {
            addNote.execute(AddCustomerNoteCommand(support, 4, "text", pinned = false))
        }
    }
}
