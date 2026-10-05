package com.dbook.presentation.catalog

import com.dbook.application.catalog.CreateAirlineUseCase
import com.dbook.application.catalog.DeleteAirlineUseCase
import com.dbook.application.catalog.ListAirlinesUseCase
import com.dbook.application.catalog.UpdateAirlineUseCase
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
@RequestMapping("${ApiPaths.V1}/admin/airlines")
@Tag(name = "Airlines (admin)", description = "The airlines flights are registered with")
@SecurityRequirement(name = "bearerAuth")
class AirlineAdminController(
    private val listAirlinesUseCase: ListAirlinesUseCase,
    private val createAirlineUseCase: CreateAirlineUseCase,
    private val updateAirlineUseCase: UpdateAirlineUseCase,
    private val deleteAirlineUseCase: DeleteAirlineUseCase,
) {
    @Operation(summary = "Lists every airline by IATA code (FLIGHT_READ)")
    @PreAuthorize("hasAuthority('FLIGHT_READ')")
    @GetMapping
    fun list(): List<AirlineResponse> = listAirlinesUseCase.execute().map(AirlineResponse::from)

    @Operation(summary = "Registers an airline; the IATA code must be new (CATALOG_WRITE)")
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @PostMapping
    fun create(
        @RequestBody request: AirlineRequest,
        authentication: Authentication,
    ): ResponseEntity<AirlineResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(
            AirlineResponse.from(createAirlineUseCase.execute(request.toCommand(authentication.currentActor()))),
        )

    @Operation(summary = "Changes an airline's code or name (CATALOG_WRITE)")
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: AirlineRequest,
        authentication: Authentication,
    ): AirlineResponse =
        AirlineResponse.from(updateAirlineUseCase.execute(id, request.toCommand(authentication.currentActor())))

    @Operation(summary = "Removes an airline no flight uses (CATALOG_WRITE)")
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: Long,
        authentication: Authentication,
    ) {
        deleteAirlineUseCase.execute(authentication.currentActor(), id)
    }
}
