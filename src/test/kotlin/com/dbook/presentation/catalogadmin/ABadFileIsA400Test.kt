package com.dbook.presentation.catalogadmin

import kotlin.test.Test
import kotlin.test.assertEquals

class ABadFileIsA400Test : FlightImportFixture() {
    @Test
    fun `given an empty file, a missing column, an unclosed quote or too many lines when importing then 400`() {
        val token = manager()

        assertEquals(400, import(token, "").response.status)
        assertEquals(400, import(token, "flightNumber,price\nA,1").response.status)
        assertEquals(400, import(token, "$header\n\"never closed,1").response.status)
        val tooMany = listOf(header) + List(5001) { line("X$it", "2060-01-01T08:00:00") }
        assertEquals(400, import(token, tooMany.joinToString("\n")).response.status)
    }
}
