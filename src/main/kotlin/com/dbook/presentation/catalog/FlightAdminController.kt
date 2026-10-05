package com.dbook.presentation.catalog

import com.dbook.application.catalog.CancelFlightUseCase
import com.dbook.application.catalog.GetAdminFlightUseCase
import com.dbook.application.catalog.RegisterFlightCommand
import com.dbook.application.catalog.RegisterFlightUseCase
import com.dbook.application.catalog.SearchAdminFlightsUseCase
import com.dbook.application.catalog.UpdateFlightUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.PageResponse
import com.dbook.presentation.common.currentActor
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `POST /admin/flights` — ADMIN only. */
@RestController
@RequestMapping("${ApiPaths.V1}/admin/flights")
@Tag(name = "Flights (admin)", description = "Flight registration")
@SecurityRequirement(name = "bearerAuth")
class FlightAdminController(
    private val registerFlightUseCase: RegisterFlightUseCase,
    private val searchAdminFlightsUseCase: SearchAdminFlightsUseCase,
    private val getAdminFlightUseCase: GetAdminFlightUseCase,
    private val updateFlightUseCase: UpdateFlightUseCase,
    private val cancelFlightUseCase: CancelFlightUseCase,
) {
    @Operation(summary = "Registers a flight, resolving origin/destination by IATA code (FLIGHT_WRITE only)")
    @PreAuthorize("hasAuthority('FLIGHT_WRITE')")
    @PostMapping
    fun register(
        @RequestBody request: RegisterFlightRequest,
        authentication: Authentication,
    ): ResponseEntity<FlightResponse> {
        val flight =
            registerFlightUseCase.execute(
                RegisterFlightCommand(
                    actor = authentication.currentActor(),
                    flightNumber = request.flightNumber,
                    airlineIataCode = request.airlineIataCode,
                    originIataCode = request.originIataCode,
                    destinationIataCode = request.destinationIataCode,
                    departureTime = request.departureTime,
                    arrivalTime = request.arrivalTime,
                    seatClass = request.seatClass,
                    price = request.price,
                    totalCapacity = request.totalCapacity,
                    aircraftType = request.aircraftType,
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(FlightResponse.from(flight))
    }

    @Operation(summary = "Lists flights by route, airline, departure period and status, soonest first (FLIGHT_READ)")
    @PreAuthorize("hasAuthority('FLIGHT_READ')")
    @GetMapping
    fun search(request: AdminFlightSearchRequest): PageResponse<AdminFlightSummaryResponse> =
        PageResponse.from(
            searchAdminFlightsUseCase.execute(request.toFilter(), request.toPage()),
            AdminFlightSummaryResponse::from,
        )

    @Operation(summary = "One flight with its seat counts, version and active bookings (FLIGHT_READ)")
    @PreAuthorize("hasAuthority('FLIGHT_READ')")
    @GetMapping("/{id}")
    fun detail(
        @PathVariable id: Long,
    ): AdminFlightDetailResponse = AdminFlightDetailResponse.from(getAdminFlightUseCase.execute(id))

    @Operation(
        summary = "Edits a flight; send the version you read (409 if someone changed it) (FLIGHT_WRITE)",
        description =
            "Capacity cannot go below the seats already booked, and the aircraft layout cannot change once a seat " +
                "was booked. More capacity adds seats at the end; less removes free seats from the end.",
    )
    @PreAuthorize("hasAuthority('FLIGHT_WRITE')")
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: UpdateFlightRequest,
        authentication: Authentication,
    ): AdminFlightDetailResponse =
        AdminFlightDetailResponse.from(
            updateFlightUseCase.execute(request.toCommand(authentication.currentActor(), id)),
        )

    @Operation(summary = "Takes a flight off sale; refused with 409 while it has active bookings (FLIGHT_WRITE)")
    @PreAuthorize("hasAuthority('FLIGHT_WRITE')")
    @PostMapping("/{id}/cancel")
    fun cancel(
        @PathVariable id: Long,
        authentication: Authentication,
    ): AdminFlightDetailResponse =
        AdminFlightDetailResponse.from(cancelFlightUseCase.execute(authentication.currentActor(), id))
}
