package com.dbook.presentation.catalog

import com.dbook.application.catalog.FeaturedDestination
import com.dbook.application.catalog.GetFeaturedDestinationsUseCase
import com.dbook.presentation.common.ApiPaths
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

/** What a destination is rated: a group of its own, so the count can join the average without another rename. */
data class RatingV2(
    val average: Double?,
)

data class PriceV2(
    val lowest: BigDecimal?,
)

/** The `/v2` shape of a destination: price and rating are objects (v1 has `lowestPrice` and `averageRating`). */
data class DestinationResponseV2(
    val iataCode: String,
    val city: String,
    val country: String,
    val photoUrl: String,
    val region: String,
    @get:JsonProperty("isPopular")
    val isPopular: Boolean,
    val price: PriceV2,
    val rating: RatingV2,
) {
    companion object {
        fun from(destination: FeaturedDestination) =
            DestinationResponseV2(
                iataCode = destination.airport.iataCode,
                city = destination.airport.city,
                country = destination.airport.country,
                photoUrl = destination.airport.photoUrl,
                region = destination.airport.region,
                isPopular = destination.airport.isPopular,
                price = PriceV2(destination.lowestPrice),
                rating = RatingV2(destination.averageRating),
            )
    }
}

/**
 * `GET /v2/destinations` — the rehearsal of a new API version (see docs/versionamento.md): the same use case as v1, a
 * different shape, both answering at once. It is public like v1.
 */
@RestController
@RequestMapping("${ApiPaths.V2}/destinations")
@Tag(name = "Destinations (public, v2)", description = "Featured destinations, price and rating as objects")
class DestinationControllerV2(
    private val getFeaturedDestinationsUseCase: GetFeaturedDestinationsUseCase,
) {
    @Operation(
        summary = "Lists every known destination with its lowest price and rating",
        description = "`rating.average` (1-5) is null until a destination has at least one review.",
    )
    @GetMapping
    fun list(): List<DestinationResponseV2> =
        getFeaturedDestinationsUseCase.execute().map { DestinationResponseV2.from(it) }
}
