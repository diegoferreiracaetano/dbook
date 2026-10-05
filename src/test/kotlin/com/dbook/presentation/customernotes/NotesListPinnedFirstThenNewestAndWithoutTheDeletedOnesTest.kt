package com.dbook.presentation.customernotes

import kotlin.test.Test
import kotlin.test.assertEquals

class NotesListPinnedFirstThenNewestAndWithoutTheDeletedOnesTest : CustomerNotesFixture() {
    @Test
    fun `given three notes, one pinned and one deleted when listing then pinned first, then newest, deleted out`() {
        val token = staffToken()
        val customerId = newCustomerId()
        addNote(token, customerId, "first")
        val pinned = noteIdOf(addNote(token, customerId, "pinned", pinned = true))
        val deleted = noteIdOf(addNote(token, customerId, "deleted"))
        addNote(token, customerId, "last")
        deleteNote(token, customerId, deleted)

        val body = bodyOf(listNotes(token, customerId))

        assertEquals(listOf("pinned", "last", "first"), body["items"].map { it["body"].asText() })
        assertEquals(3, body["totalElements"].asInt())
        assertEquals(pinned, body["items"][0]["id"].asLong())
    }
}
