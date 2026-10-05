package com.dbook.presentation.customersearch

import kotlin.test.Test
import kotlin.test.assertEquals

class ABadSearchParameterIsAValidationErrorTest : CustomerSearchFixture() {
    @Test
    fun `given an unknown sort, a size of 101 or an inverted period when searching then it is 400 VALIDATION_FAILED`() {
        val token = supportToken()

        listOf(
            "sort" to "PASSWORD",
            "size" to "101",
            "createdFrom" to "2026-02-01T00:00:00Z",
        ).forEach { (name, value) ->
            val extra = if (name == "createdFrom") arrayOf("createdTo" to "2026-01-01T00:00:00Z") else emptyArray()
            val result = search(token, name to value, *extra)

            assertEquals(400, result.response.status, "$name=$value")
            assertEquals("VALIDATION_FAILED", errorCodeOf(result))
        }
    }
}
