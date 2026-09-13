package com.dbook.presentation

import com.dbook.application.GetFeaturedDestinationsUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * `GET /destinations` — public, no authentication required. Every known destination (airport,
 * photo, real lowest price) in one response — the mobile client renders this list as-is, with
 * no hardcoded airport data of its own.
 */
@RestController
@RequestMapping("/destinations")
@Tag(name = "Destinations (public)", description = "Featured destinations with real pricing")
class DestinationController(
    private val getFeaturedDestinationsUseCase: GetFeaturedDestinationsUseCase,
) {
    @Operation(summary = "Lists every known destination with its real lowest price")
    @GetMapping
    fun list(): List<DestinationResponse> =
        getFeaturedDestinationsUseCase.execute().map { DestinationResponse.from(it) }
}
