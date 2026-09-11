package com.dbook.presentation

import com.dbook.application.GetSeatMapUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `GET /bookables/{id}/seats` — public, no authentication required (same spirit as `/flights/search`). */
@RestController
@RequestMapping("/bookables")
@Tag(name = "Bookables (public)", description = "Public seat map lookup")
class BookableController(
    private val getSeatMapUseCase: GetSeatMapUseCase,
) {
    @Operation(summary = "Returns the seat map of a Bookable (e.g. a flight)")
    @GetMapping("/{id}/seats")
    fun seats(
        @PathVariable id: Long,
    ): List<SeatResponse> = getSeatMapUseCase.execute(id).map { SeatResponse.from(it) }
}
