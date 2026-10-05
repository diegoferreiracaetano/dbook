package com.dbook.presentation.customersearch

import kotlin.test.Test
import kotlin.test.assertEquals

class ASearchIgnoresCaseAndAccentsTest : CustomerSearchFixture() {
    @Test
    fun `given José when searching for JOSE in capitals then he is found`() {
        val tag = newTag()
        newCustomer("José $tag")
        newCustomer("Maria $tag")

        val result = search(supportToken(), "query" to "JOSE $tag")

        assertEquals(listOf("José $tag"), namesOf(result))
        assertEquals(1, bodyOf(result)["totalElements"].asInt())
    }
}
