package com.dbook.presentation.flight

import com.dbook.application.flight.FlightImportCommand
import com.dbook.application.flight.ImportFlightsUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.currentActor
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiPaths.V1}/admin/flights/import")
@Tag(name = "Flights (admin)", description = "Flight registration")
@SecurityRequirement(name = "bearerAuth")
class FlightImportController(
    private val importFlightsUseCase: ImportFlightsUseCase,
) {
    @Operation(
        summary = "Imports flights from a CSV (text/csv body), all or nothing; a dry run by default (FLIGHT_WRITE)",
        description =
            "Columns: flightNumber, airlineIataCode, originIataCode, destinationIataCode, departureTime, " +
                "arrivalTime, seatClass, price, totalCapacity, aircraftType. At most 5000 lines. Every error comes " +
                "back with its line. Send `dryRun=false` to write: it writes only if there are no errors (otherwise " +
                "422). A flight that already exists (same number and departure) is skipped, so sending the file " +
                "twice is safe.",
    )
    @PreAuthorize("hasAuthority('FLIGHT_WRITE')")
    @PostMapping(consumes = ["text/csv"])
    fun import(
        @RequestParam(defaultValue = "true") dryRun: Boolean,
        @RequestBody csv: String,
        authentication: Authentication,
    ): ResponseEntity<FlightImportResponse> {
        val rows = CsvParser.parse(csv).toImportRows()
        val result = importFlightsUseCase.execute(FlightImportCommand(authentication.currentActor(), rows, dryRun))
        val status = if (!dryRun && result.errors.isNotEmpty()) HttpStatus.UNPROCESSABLE_ENTITY else HttpStatus.OK
        return ResponseEntity.status(status).body(FlightImportResponse.from(result))
    }
}
