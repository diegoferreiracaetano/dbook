package com.dbook.presentation.catalogadmin

import kotlin.test.Test
import kotlin.test.assertEquals

class ACommitWithAnErrorWritesNothingAndIsA422Test : FlightImportFixture() {
    @Test
    fun `given a file with one bad line when committing then 422 and not even the good lines are created`() {
        val token = manager()
        val day = "2052-04-1${(0..9).random()}"
        val csv =
            listOf(
                header,
                line("GOOD1", "${day}T08:00:00"),
                line("BAD1", "${day}T08:00:00", mapOf("airlineIataCode" to "ZZ")),
            ).joinToString("\n")

        val result = import(token, csv, dryRun = false)

        assertEquals(422, result.response.status)
        assertEquals(0, json(result)["created"].asInt())
        assertEquals(emptyList(), flightsOn(token, day))
    }
}
