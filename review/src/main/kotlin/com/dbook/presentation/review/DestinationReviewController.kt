package com.dbook.presentation.review

import com.dbook.application.review.ListDestinationReviewsUseCase
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

/** `GET /destinations/{iata}/reviews` — public, no authentication required. */
@RestController
@RequestMapping("${ApiPaths.V1}/destinations")
@Tag(name = "Reviews (public)", description = "What travellers say about a destination")
class DestinationReviewController(
    private val listDestinationReviewsUseCase: ListDestinationReviewsUseCase,
) {
    @Operation(
        summary = "How a destination is rated and its reviews, one page at a time",
        description = "Only visible reviews count. Authors show as first name and the initial of the last one.",
    )
    @GetMapping("/{iata}/reviews")
    fun reviews(
        @PathVariable iata: String,
        @RequestParam(defaultValue = "RECENT") sort: ReviewSort,
        params: PageParams,
    ): DestinationReviewsResponse =
        DestinationReviewsResponse.from(listDestinationReviewsUseCase.execute(iata, sort, params.toQuery()))
}
