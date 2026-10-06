package com.dbook.presentation.customernotes

import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class OnlyTheAuthorEditsAndOnlyAuthorOrSuperAdminDeletesTest : CustomerNotesFixture() {
    @Test
    fun `given a note when a colleague edits or deletes it then 403, and a SUPER_ADMIN can delete it`() {
        val author = staffToken()
        val colleague = staffToken()
        val customerId = newCustomerId()
        val noteId = noteIdOf(addNote(author, customerId, "mine"))

        assertEquals(403, editNote(colleague, customerId, noteId, mapOf("body" to "hijack")).response.status)
        assertEquals(403, deleteNote(colleague, customerId, noteId).response.status)
        assertEquals(200, editNote(author, customerId, noteId, mapOf("pinned" to true)).response.status)
        assertEquals(204, deleteNote(staffToken(Role.SUPER_ADMIN), customerId, noteId).response.status)
        assertEquals(404, deleteNote(author, customerId, noteId).response.status)
    }
}
