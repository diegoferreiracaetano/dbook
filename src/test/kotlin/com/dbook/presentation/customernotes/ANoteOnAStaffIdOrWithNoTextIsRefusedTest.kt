package com.dbook.presentation.customernotes

import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class ANoteOnAStaffIdOrWithNoTextIsRefusedTest : CustomerNotesFixture() {
    @Test
    fun `given a staff id or a blank text when writing a note then it is 404 and 400`() {
        val token = staffToken()
        val staffEmail = uniqueEmail()
        registerStaff(staffEmail, Role.SUPPORT)

        assertEquals(404, addNote(token, userIdOf(staffEmail), "text").response.status)
        assertEquals(400, addNote(token, newCustomerId(), "   ").response.status)
    }
}
