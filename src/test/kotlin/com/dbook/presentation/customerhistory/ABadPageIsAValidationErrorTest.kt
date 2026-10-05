package com.dbook.presentation.customerhistory

import kotlin.test.Test
import kotlin.test.assertEquals

class ABadPageIsAValidationErrorTest : CustomerHistoryFixture() {
    @Test
    fun `given a size of 101 or a negative page when listing then it is 400 VALIDATION_FAILED`() {
        val email = uniqueEmail()
        registerAndLogin(email)
        val token = supportToken()

        listOf("size" to "101", "page" to "-1").forEach { (name, value) ->
            val result = history(token, userIdOf(email), "bookings", name to value)

            assertEquals(400, result.response.status, "$name=$value")
            assertEquals("VALIDATION_FAILED", errorCodeOf(result))
        }
    }
}
