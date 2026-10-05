package com.dbook.presentation.accommodation

import com.dbook.application.accommodation.AddRoomTypeUseCase
import com.dbook.application.accommodation.CreateAccommodationUseCase
import com.dbook.application.accommodation.ListAccommodationsUseCase
import com.dbook.application.accommodation.SetAccommodationActiveUseCase
import com.dbook.application.accommodation.UpdateAccommodationUseCase
import com.dbook.application.accommodation.UpdateRoomTypeUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.PageParams
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** `/admin/accommodations` — the team's CRUD of hotels, their room types and rates (CATALOG_WRITE). */
@RestController
@RequestMapping("${ApiPaths.V1}/admin/accommodations")
@Tag(name = "Accommodations (admin)", description = "Hotels, room types and rates")
@SecurityRequirement(name = "bearerAuth")
class AdminAccommodationController(
    private val createAccommodationUseCase: CreateAccommodationUseCase,
    private val updateAccommodationUseCase: UpdateAccommodationUseCase,
    private val setAccommodationActiveUseCase: SetAccommodationActiveUseCase,
    private val addRoomTypeUseCase: AddRoomTypeUseCase,
    private val updateRoomTypeUseCase: UpdateRoomTypeUseCase,
    private val listAccommodationsUseCase: ListAccommodationsUseCase,
) {
    @Operation(
        summary = "Creates a hotel with its room types; 404 if the destination airport does not exist (CATALOG_WRITE)",
    )
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @PostMapping
    fun create(
        @RequestBody request: CreateAccommodationRequest,
        authentication: Authentication,
    ): ResponseEntity<AccommodationResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(
            AccommodationResponse.from(
                createAccommodationUseCase.execute(
                    authentication.currentActor(),
                    request.accommodation.toDetails(),
                    request.roomTypes.orEmpty().map { it.toData() },
                ),
            ),
        )

    @Operation(summary = "Every hotel, newest first, on sale or not (CATALOG_WRITE)")
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @GetMapping
    fun list(
        @RequestParam(required = false) destination: String?,
        params: PageParams,
    ): PageResponse<AccommodationResponse> =
        PageResponse.from(
            listAccommodationsUseCase.search(destination, params.toQuery()),
        ) { AccommodationResponse.from(it) }

    @Operation(summary = "One hotel with all its room types (CATALOG_WRITE)")
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @GetMapping("/{id}")
    fun get(
        @PathVariable id: Long,
    ): AccommodationResponse = AccommodationResponse.from(listAccommodationsUseCase.find(id))

    @Operation(summary = "Changes what describes the hotel (CATALOG_WRITE)")
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: AccommodationRequest,
        authentication: Authentication,
    ): AccommodationResponse =
        AccommodationResponse.from(
            updateAccommodationUseCase.execute(authentication.currentActor(), id, request.toDetails()),
        )

    @Operation(
        summary = "Takes the hotel off sale: no more showing or bookings; the bookings made stay (CATALOG_WRITE)",
    )
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @PostMapping("/{id}/deactivate")
    fun deactivate(
        @PathVariable id: Long,
        authentication: Authentication,
    ): AccommodationResponse =
        AccommodationResponse.from(
            setAccommodationActiveUseCase.execute(authentication.currentActor(), id, active = false),
        )

    @Operation(summary = "Puts the hotel back on sale (CATALOG_WRITE)")
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @PostMapping("/{id}/activate")
    fun activate(
        @PathVariable id: Long,
        authentication: Authentication,
    ): AccommodationResponse =
        AccommodationResponse.from(
            setAccommodationActiveUseCase.execute(authentication.currentActor(), id, active = true),
        )

    @Operation(summary = "Adds a room type; 409 if the name exists in this hotel (CATALOG_WRITE)")
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @PostMapping("/{id}/room-types")
    fun addRoomType(
        @PathVariable id: Long,
        @RequestBody request: RoomTypeRequest,
        authentication: Authentication,
    ): ResponseEntity<AccommodationResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(
            AccommodationResponse.from(addRoomTypeUseCase.execute(authentication.currentActor(), id, request.toData())),
        )

    @Operation(
        summary = "Changes a room type's rate, capacity, name, quantity or whether it is on sale (CATALOG_WRITE)",
        description = "The quantity cannot go below the most rooms already booked on any night from today on (409).",
    )
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @PutMapping("/{id}/room-types/{roomTypeId}")
    fun updateRoomType(
        @PathVariable id: Long,
        @PathVariable roomTypeId: Long,
        @RequestBody request: RoomTypeRequest,
        authentication: Authentication,
    ): AccommodationResponse =
        AccommodationResponse.from(
            updateRoomTypeUseCase.execute(authentication.currentActor(), id, roomTypeId, request.toData()),
        )
}
