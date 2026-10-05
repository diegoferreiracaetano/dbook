package com.dbook.presentation.catalogadmin

import kotlin.test.Test
import kotlin.test.assertEquals

class ADryRunReportsAndWritesNothingTest : FlightImportFixture() {
    @Test
    fun `given a valid file when importing without dryRun then it only reports what it would create`() {
        val token = manager()
        val day = "20${(41..49).random()}-0${(1..9).random()}-1${(0..9).random()}"
        val csv = listOf(header, line("IMP1", "${day}T08:00:00"), line("IMP2", "${day}T08:00:00")).joinToString("\n")

        val result = import(token, csv)

        assertEquals(200, result.response.status)
        assertEquals(true, json(result)["dryRun"].asBoolean())
        assertEquals(2, json(result)["toCreate"].asInt())
        assertEquals(0, json(result)["created"].asInt())
        assertEquals(emptyList(), flightsOn(token, day))
    }
}
