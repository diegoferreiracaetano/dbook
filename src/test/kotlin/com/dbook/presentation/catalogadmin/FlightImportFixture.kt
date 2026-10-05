package com.dbook.presentation.catalogadmin

import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.post

abstract class FlightImportFixture : CatalogAdminFixture() {
    protected val header =
        "flightNumber,airlineIataCode,originIataCode,destinationIataCode,departureTime,arrivalTime," +
            "seatClass,price,totalCapacity,aircraftType"

    /** One data line: sensible defaults for every column, with [overrides] by column name. */
    protected fun line(
        number: String,
        departure: String,
        overrides: Map<String, String> = emptyMap(),
    ): String {
        val cells =
            mapOf(
                "flightNumber" to number, "airlineIataCode" to "LA", "originIataCode" to "GRU",
                "destinationIataCode" to "GIG", "departureTime" to departure,
                "arrivalTime" to departure.replace("T08", "T09"), "seatClass" to "ECONOMY", "price" to "100.00",
                "totalCapacity" to "6", "aircraftType" to "Airbus A320",
            ) + overrides
        return header.split(",").joinToString(",") { cells.getValue(it) }
    }

    protected fun import(
        token: String,
        csv: String,
        dryRun: Boolean? = null,
    ): MvcResult =
        mockMvc.post("/v1/admin/flights/import" + dryRun?.let { "?dryRun=$it" }.orEmpty()) {
            header("Authorization", "Bearer $token")
            contentType = org.springframework.http.MediaType.parseMediaType("text/csv")
            content = csv
        }.andReturn()

    protected fun flightsOn(
        token: String,
        day: String,
    ): List<String> =
        json(searchFlights(token, "departureFrom" to "${day}T00:00:00", "departureTo" to "${day}T23:59:59"))["items"]
            .map { it["flightNumber"].asText() }
}
