package com.dbook.presentation.customernotes

import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class OnlyWhoCanNoteCanWriteAndWhoCanReadCanListTest : CustomerNotesFixture() {
    @Test
    fun `given a catalog manager when writing or listing notes then it is 403`() {
        val catalog = staffToken(Role.CATALOG_MANAGER)
        val customerId = newCustomerId()

        assertEquals(403, addNote(catalog, customerId, "text").response.status)
        assertEquals(403, listNotes(catalog, customerId).response.status)
        assertEquals(201, addNote(staffToken(), customerId, "text").response.status)
    }
}
