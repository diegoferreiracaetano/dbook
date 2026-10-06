package com.dbook.presentation.trips

import com.dbook.application.trips.ListMyBookingsUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `GET /bookings`: the customer's own trips. It shares the path of the booking routes, and lives apart from them. */
@RestController
@RequestMapping("${ApiPaths.V1}/bookings")
@Tag(name = "Bookings", description = "Booking and cancellation of bookable items (flights, and hotels in the future)")
@SecurityRequirement(name = "bearerAuth")
class TripsController(
    private val listMyBookingsUseCase: ListMyBookingsUseCase,
) {
    @Operation(summary = "Lists every booking made by the authenticated user (\"my trips\")")
    @GetMapping
    fun list(authentication: Authentication): List<MyBookingResponse> =
        listMyBookingsUseCase.execute(authentication.currentUserId()).map { MyBookingResponse.from(it) }
}
