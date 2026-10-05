package com.dbook.presentation.catalog

import com.dbook.application.catalog.CreateAirportUseCase
import com.dbook.application.catalog.DeleteAirportUseCase
import com.dbook.application.catalog.ListAirportsUseCase
import com.dbook.application.catalog.UpdateAirportUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.currentActor
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiPaths.V1}/admin/airports")
@Tag(name = "Airports (admin)", description = "The airports flights leave from and arrive at")
@SecurityRequirement(name = "bearerAuth")
class AirportAdminController(
    private val listAirportsUseCase: ListAirportsUseCase,
    private val createAirportUseCase: CreateAirportUseCase,
    private val updateAirportUseCase: UpdateAirportUseCase,
    private val deleteAirportUseCase: DeleteAirportUseCase,
) {
    @Operation(summary = "Lists every airport by IATA code (FLIGHT_READ)")
    @PreAuthorize("hasAuthority('FLIGHT_READ')")
    @GetMapping
    fun list(): List<AirportResponse> = listAirportsUseCase.execute().map(AirportResponse::from)

    @Operation(summary = "Registers an airport; the IATA code must be new (CATALOG_WRITE)")
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @PostMapping
    fun create(
        @RequestBody request: AirportRequest,
        authentication: Authentication,
    ): ResponseEntity<AirportResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(
            AirportResponse.from(createAirportUseCase.execute(request.toCommand(authentication.currentActor()))),
        )

    @Operation(summary = "Changes an airport (CATALOG_WRITE)")
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: AirportRequest,
        authentication: Authentication,
    ): AirportResponse =
        AirportResponse.from(updateAirportUseCase.execute(id, request.toCommand(authentication.currentActor())))

    @Operation(summary = "Removes an airport no flight uses (CATALOG_WRITE)")
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: Long,
        authentication: Authentication,
    ) {
        deleteAirportUseCase.execute(authentication.currentActor(), id)
    }
}
