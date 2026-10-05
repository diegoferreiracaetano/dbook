package com.dbook.presentation.catalog

import com.dbook.domain.seating.KNOWN_AIRCRAFT_TYPES
import com.dbook.domain.seating.seatLayoutFor
import com.dbook.presentation.common.ApiPaths
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class AircraftModelResponse(
    val name: String,
    val seatLayout: List<Int>,
    val seatsPerRow: Int,
)

/** `GET /admin/aircraft-models` — read-only: the aircraft the flight form can offer, with the layout each one makes. */
@RestController
@RequestMapping("${ApiPaths.V1}/admin/aircraft-models")
@Tag(name = "Aircraft models (admin)", description = "The aircraft with a known seat layout")
@SecurityRequirement(name = "bearerAuth")
class AircraftModelController {
    @Operation(summary = "Lists the aircraft models and their seat layouts (FLIGHT_READ)")
    @PreAuthorize("hasAuthority('FLIGHT_READ')")
    @GetMapping
    fun list(): List<AircraftModelResponse> =
        KNOWN_AIRCRAFT_TYPES.map { AircraftModelResponse(it, seatLayoutFor(it), seatLayoutFor(it).sum()) }
}
