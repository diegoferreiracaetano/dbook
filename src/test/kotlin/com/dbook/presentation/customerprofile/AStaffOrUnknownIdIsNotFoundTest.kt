package com.dbook.presentation.customerprofile

import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class AStaffOrUnknownIdIsNotFoundTest : CustomerProfileFixture() {
    @Test
    fun `given the id of a staff member or of nobody when reading the profile then both are 404 NOT_FOUND`() {
        val staffEmail = uniqueEmail()
        registerStaff(staffEmail, Role.SUPPORT)
        val token = supportToken()

        listOf(userIdOf(staffEmail), 999_999_999L).forEach { id ->
            val result = profile(token, id)

            assertEquals(404, result.response.status)
            assertEquals("NOT_FOUND", errorCodeOf(result))
        }
    }
}
