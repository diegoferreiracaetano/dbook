package com.dbook.presentation.accommodation

import com.dbook.application.accommodation.ListAccommodationsUseCase
import com.dbook.application.accommodation.RegisterStayBookingCommand
import com.dbook.application.accommodation.RegisterStayBookingUseCase
import com.dbook.application.accommodation.SearchAccommodationsUseCase
import com.dbook.application.review.ListAccommodationReviewsUseCase
import com.dbook.domain.accommodation.AccommodationNotFoundException
import com.dbook.domain.accommodation.AccommodationSearch
import com.dbook.domain.review.ReviewSort
import com.dbook.presentation.booking.BookingResponse
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.PageParams
import com.dbook.presentation.common.PageResponse
import com.dbook.presentation.common.currentUserId
import com.dbook.presentation.review.DestinationReviewsResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/** `/accommodations` — searching and reading hotels is public; booking a stay needs a signed-in customer. */
@RestController
@RequestMapping("${ApiPaths.V1}/accommodations")
@Tag(name = "Accommodations", description = "Hotels: search, details, reviews and booking a stay")
class AccommodationController(
    private val searchAccommodationsUseCase: SearchAccommodationsUseCase,
    private val listAccommodationsUseCase: ListAccommodationsUseCase,
    private val listAccommodationReviewsUseCase: ListAccommodationReviewsUseCase,
    private val registerStayBookingUseCase: RegisterStayBookingUseCase,
) {
    @Operation(
        summary = "Hotels at a destination with a room free for every night, cheapest stay first (public)",
        description = "Each hotel lists the room types free for the whole stay and what the stay costs in each.",
    )
    @GetMapping("/search")
    fun search(
        @RequestParam destination: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) checkIn: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) checkOut: LocalDate,
        @RequestParam(defaultValue = "2") guests: Int,
        params: PageParams,
    ): PageResponse<AccommodationResultResponse> =
        PageResponse.from(
            searchAccommodationsUseCase.execute(
                AccommodationSearch(destination, checkIn, checkOut, guests),
                params.toQuery(),
            ),
            AccommodationResultResponse::from,
        )

    @Operation(summary = "One hotel with its room types on sale (public); 404 if it does not exist or is off sale")
    @GetMapping("/{id}")
    fun get(
        @PathVariable id: Long,
    ): AccommodationResponse {
        val hotel = listAccommodationsUseCase.find(id)
        if (!hotel.active) throw AccommodationNotFoundException(id)
        return AccommodationResponse.from(hotel, onlyActiveRooms = true)
    }

    @Operation(summary = "How a hotel is rated and what its guests wrote (public)")
    @GetMapping("/{id}/reviews")
    fun reviews(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "RECENT") sort: ReviewSort,
        params: PageParams,
    ): DestinationReviewsResponse =
        DestinationReviewsResponse.from(listAccommodationReviewsUseCase.execute(id, sort, params.toQuery()))

    @Operation(
        summary = "Books a room for some nights (PENDING for 15 minutes, like a seat); pay it with POST /payments",
        description = "409 when a night is full, 404 for an unknown room type, 400 for bad dates or too many guests.",
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{id}/bookings")
    fun book(
        @PathVariable id: Long,
        @RequestBody request: StayBookingRequest,
        authentication: Authentication,
    ): ResponseEntity<BookingResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(
            BookingResponse.from(
                registerStayBookingUseCase.execute(
                    RegisterStayBookingCommand(
                        id,
                        request.roomTypeId,
                        request.checkIn,
                        request.checkOut,
                        request.guests,
                        authentication.currentUserId(),
                    ),
                ),
            ),
        )
}
