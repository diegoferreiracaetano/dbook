package com.dbook.presentation.flight

import com.dbook.application.flight.RegisterBookingCommand
import com.dbook.application.flight.RegisterBookingUseCase
import com.dbook.presentation.booking.BookingResponse
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `POST /bookings`: books a seat of a flight. It shares the path of the booking routes, and lives with the flights. */
@RestController
@RequestMapping("${ApiPaths.V1}/bookings")
@Tag(name = "Bookings", description = "Booking and cancellation of bookable items (flights, and hotels in the future)")
@SecurityRequirement(name = "bearerAuth")
class FlightBookingController(
    private val registerBookingUseCase: RegisterBookingUseCase,
) {
    @Operation(summary = "Creates a booking (PENDING) for a Bookable, decrementing its availability")
    @PostMapping
    fun register(
        @RequestBody request: RegisterBookingRequest,
        authentication: Authentication,
    ): ResponseEntity<BookingResponse> {
        val booking =
            registerBookingUseCase.execute(
                RegisterBookingCommand(
                    bookableId = request.bookableId,
                    seatId = request.seatId,
                    customerId = authentication.currentUserId(),
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingResponse.from(booking))
    }
}
