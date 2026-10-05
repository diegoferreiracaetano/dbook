package com.dbook.presentation.customersearch

import kotlin.test.Test
import kotlin.test.assertEquals

class SortsAndPagesWithTheTotalTest : CustomerSearchFixture() {
    @Test
    fun `given three customers when sorting by name desc in pages of two then pages and total are right`() {
        val tag = newTag()
        listOf("C1", "C2", "C3").forEach { newCustomer("$it $tag") }
        val token = supportToken()
        val sort = arrayOf("query" to tag, "sort" to "NAME", "direction" to "DESC", "size" to "2")

        val first = search(token, *sort, "page" to "0")
        val second = search(token, *sort, "page" to "1")

        assertEquals(listOf("C3 $tag", "C2 $tag"), namesOf(first))
        assertEquals(listOf("C1 $tag"), namesOf(second))
        assertEquals(3, bodyOf(first)["totalElements"].asInt())
        assertEquals(2, bodyOf(first)["totalPages"].asInt())
    }
}
