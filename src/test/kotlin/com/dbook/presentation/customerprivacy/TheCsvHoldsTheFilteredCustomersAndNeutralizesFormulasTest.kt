package com.dbook.presentation.customerprivacy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TheCsvHoldsTheFilteredCustomersAndNeutralizesFormulasTest : CustomerPrivacyFixture() {
    @Test
    fun `given two customers when exporting by their tag then the CSV has them and the formula is neutralized`() {
        val tag = newTag()
        newCustomer("Ana $tag")
        newCustomer("=cmd $tag")

        val result = exportCsv(superAdminToken(), "query" to tag, "sort" to "NAME", "direction" to "ASC")

        assertEquals(200, result.response.status)
        assertTrue(result.response.contentType.orEmpty().startsWith("text/csv"))
        assertTrue(result.response.getHeader("Content-Disposition").orEmpty().contains("customers.csv"))
        val lines = csvOf(result)
        assertEquals("id,name,email,status,createdAt,lastLoginAt,bookingCount", lines[0])
        assertEquals(3, lines.size)
        assertTrue(lines[1].contains(",'=cmd $tag,"), lines[1])
        assertTrue(lines[2].contains(",Ana $tag,"), lines[2])
    }
}
