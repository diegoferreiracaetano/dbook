package com.dbook.presentation.customerhistory

import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class AStaffOrUnknownIdIsNotFoundOnEveryListTest : CustomerHistoryFixture() {
    @Test
    fun `given the id of a staff member or of nobody when listing the history then every list is 404 NOT_FOUND`() {
        val staffEmail = uniqueEmail()
        registerStaff(staffEmail, Role.SUPPORT)
        val token = supportToken()

        listOf(userIdOf(staffEmail), 999_999_999L).forEach { id ->
            listOf("bookings", "payments", "reviews").forEach { list ->
                val result = history(token, id, list)

                assertEquals(404, result.response.status, "$id/$list")
                assertEquals("NOT_FOUND", errorCodeOf(result))
            }
        }
    }
}
