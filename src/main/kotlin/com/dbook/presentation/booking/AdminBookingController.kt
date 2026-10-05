package com.dbook.presentation.booking

import com.dbook.application.booking.GetAdminBookingUseCase
import com.dbook.application.booking.SearchAdminBookingsUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `/admin/bookings` — every customer's bookings, for the support team (the customer's own are `/bookings`). */
@RestController
@RequestMapping("${ApiPaths.V1}/admin/bookings")
@Tag(name = "Bookings (admin)", description = "Looking up any customer's booking")
@SecurityRequirement(name = "bearerAuth")
class AdminBookingController(
    private val searchAdminBookingsUseCase: SearchAdminBookingsUseCase,
    private val getAdminBookingUseCase: GetAdminBookingUseCase,
) {
    @Operation(summary = "Searches bookings by status, flight, customer, period and paid or not, newest first")
    @PreAuthorize("hasAuthority('BOOKING_READ_ANY')")
    @GetMapping
    fun search(request: AdminBookingSearchRequest): PageResponse<AdminBookingSummaryResponse> =
        PageResponse.from(
            searchAdminBookingsUseCase.execute(request.toFilter(), request.toPage()),
            AdminBookingSummaryResponse::from,
        )

    @Operation(summary = "One booking with its payment, its refund and the timeline of its statuses")
    @PreAuthorize("hasAuthority('BOOKING_READ_ANY')")
    @GetMapping("/{id}")
    fun detail(
        @PathVariable id: Long,
    ): AdminBookingDetailResponse = AdminBookingDetailResponse.from(getAdminBookingUseCase.execute(id))
}
