package com.dbook.presentation.customersearch

import kotlin.test.Test
import kotlin.test.assertEquals

class APercentSignInTheSearchIsLiteralTest : CustomerSearchFixture() {
    @Test
    fun `given a search with a percent sign when searching then it matches the character, not any text`() {
        val tag = newTag()
        newCustomer("Ana 50%x$tag")
        newCustomer("Ana 505x$tag")

        val result = search(supportToken(), "query" to "50%x$tag")

        assertEquals(listOf("Ana 50%x$tag"), namesOf(result))
    }
}
