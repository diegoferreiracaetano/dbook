package com.dbook.presentation.catalog

import com.dbook.application.catalog.GetFeaturedDestinationsUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.DeprecatedApi
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * `GET /destinations` — public, no authentication required. Every known destination (airport,
 * photo, real lowest price, average rating) in one response — the mobile client renders this list as-is, with
 * no hardcoded airport data of its own.
 */
@RestController
@DeprecatedApi(
    since = "2026-10-05",
    sunset = "2027-10-05",
    link = "https://github.com/diegoferreiracaetano/dbook/blob/main/docs/versionamento.md#o-ensaio-de-uma-v2",
)
@RequestMapping("${ApiPaths.V1}/destinations")
@Tag(name = "Destinations (public)", description = "Featured destinations with real pricing and ratings")
class DestinationController(
    private val getFeaturedDestinationsUseCase: GetFeaturedDestinationsUseCase,
) {
    @Operation(
        summary = "Lists every known destination with its real lowest price and average rating",
        description = "averageRating (1-5) is null until a destination has at least one review.",
    )
    @GetMapping
    fun list(): List<DestinationResponse> =
        getFeaturedDestinationsUseCase.execute().map { DestinationResponse.from(it) }
}
