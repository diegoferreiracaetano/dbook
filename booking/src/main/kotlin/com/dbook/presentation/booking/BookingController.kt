package com.dbook.presentation.booking

import com.dbook.application.booking.CancelBookingUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.currentRole
import com.dbook.presentation.common.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `POST /bookings/{id}/cancel` — authenticated; cancelling requires the owner or BOOKING_CANCEL_ANY. */
@RestController
@RequestMapping("${ApiPaths.V1}/bookings")
@Tag(name = "Bookings", description = "Booking and cancellation of bookable items (flights, and hotels in the future)")
@SecurityRequirement(name = "bearerAuth")
class BookingController(
    private val cancelBookingUseCase: CancelBookingUseCase,
) {
    @Operation(summary = "Cancels a PENDING booking (owner or BOOKING_CANCEL_ANY only), returning the availability")
    @PostMapping("/{id}/cancel")
    fun cancel(
        @PathVariable id: Long,
        authentication: Authentication,
    ): ResponseEntity<BookingResponse> {
        val booking = cancelBookingUseCase.execute(id, authentication.currentUserId(), authentication.currentRole())
        return ResponseEntity.ok(BookingResponse.from(booking))
    }
}
