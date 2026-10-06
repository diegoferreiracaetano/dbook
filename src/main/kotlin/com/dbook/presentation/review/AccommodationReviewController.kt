package com.dbook.presentation.review

import com.dbook.application.review.ListAccommodationReviewsUseCase
import com.dbook.domain.review.ReviewSort
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.PageParams
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** `GET /accommodations/{id}/reviews`: what a hotel's guests wrote. It shares the path of the hotels, and lives apart. */
@RestController
@RequestMapping("${ApiPaths.V1}/accommodations")
@Tag(name = "Accommodations", description = "Hotels: search, details, reviews and booking a stay")
class AccommodationReviewController(
    private val listAccommodationReviewsUseCase: ListAccommodationReviewsUseCase,
) {
    @Operation(summary = "How a hotel is rated and what its guests wrote (public)")
    @GetMapping("/{id}/reviews")
    fun reviews(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "RECENT") sort: ReviewSort,
        params: PageParams,
    ): DestinationReviewsResponse =
        DestinationReviewsResponse.from(listAccommodationReviewsUseCase.execute(id, sort, params.toQuery()))
}
