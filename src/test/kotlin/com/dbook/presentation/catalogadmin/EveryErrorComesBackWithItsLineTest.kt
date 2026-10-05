package com.dbook.presentation.catalogadmin

import kotlin.test.Test
import kotlin.test.assertEquals

class EveryErrorComesBackWithItsLineTest : FlightImportFixture() {
    @Test
    fun `given a file with several bad lines when checking it then every error comes with its line`() {
        val token = manager()
        val day = "2051-03-1${(0..9).random()}"
        val csv =
            listOf(
                header,
                line("OK1", "${day}T08:00:00"),
                line("BAD1", "${day}T08:00:00", mapOf("airlineIataCode" to "ZZ")),
                line("BAD2", "not-a-date"),
                line("BAD3", "${day}T08:00:00", mapOf("arrivalTime" to "${day}T07:00:00")),
                line("BAD4", "${day}T08:00:00", mapOf("destinationIataCode" to "GRU")),
                line("BAD5", "${day}T08:00:00", mapOf("totalCapacity" to "0")),
                line("OK1", "${day}T08:00:00"),
            ).joinToString("\r\n")

        val result = json(import(token, csv))

        assertEquals(1, result["toCreate"].asInt())
        assertEquals(listOf(3, 4, 5, 6, 7, 8), result["errors"].map { it["line"].asInt() })
        assertEquals("the same flight and departure as line 2", result["errors"].last()["message"].asText())
    }
}
