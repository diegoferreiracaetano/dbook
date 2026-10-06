package com.dbook.presentation.catalogadmin

import kotlin.test.Test
import kotlin.test.assertEquals

class ACommitCreatesTheFlightsAndSendingItAgainCreatesNothingTest : FlightImportFixture() {
    @Test
    fun `given a valid file when committing twice then the flights are created once, with their seats, and audited`() {
        val token = manager()
        val day = "2053-05-1${(0..9).random()}"
        val csv =
            listOf(
                header,
                line("NEW1", "${day}T08:00:00", mapOf("totalCapacity" to "9")),
                line("NEW2", "${day}T08:00:00"),
            ).joinToString("\n")

        val first = json(import(token, csv, dryRun = false))
        val second = json(import(token, csv, dryRun = false))

        assertEquals(2, first["created"].asInt())
        assertEquals(0, second["created"].asInt())
        assertEquals(2, second["alreadyExisting"].asInt())
        assertEquals(setOf("NEW1", "NEW2"), flightsOn(token, day).toSet())
        val newId =
            json(
                searchFlights(token, "departureFrom" to "${day}T00:00:00", "departureTo" to "${day}T23:59:59"),
            )["items"]
                .first { it["flightNumber"].asText() == "NEW1" }["id"].asLong()
        assertEquals(9, seatLabels(newId).size)
        val admin = registerStaffAndLogin(uniqueEmail(), com.dbook.domain.common.access.Role.SUPER_ADMIN)
        assertEquals(true, auditEntries(admin, "action=FLIGHTS_IMPORTED&targetId=import").size() >= 1)
    }
}
