package com.dbook.presentation.review

import com.dbook.application.review.CreateReviewCommand
import com.dbook.application.review.CreateReviewUseCase
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

/** `POST /reviews` — authenticated; rates one of the caller's own CONFIRMED bookings. */
@RestController
@RequestMapping("/reviews")
@Tag(name = "Reviews", description = "rates one of the caller's own CONFIRMED booking")
@SecurityRequirement(name = "bearerAuth")
class ReviewController(
    private val createReviewUseCase: CreateReviewUseCase,
) {
    @Operation(summary = "rates one of the caller's own CONFIRMED booking")
    @PostMapping
    fun register(
        @RequestBody request: RegisterReviewRequest,
        authentication: Authentication,
    ): ResponseEntity<ReviewResponse> {
        val review =
            createReviewUseCase.execute(
                CreateReviewCommand(
                    customerId = authentication.currentUserId(),
                    bookingId = request.bookingId,
                    rating = request.rating,
                    comment = request.comment,
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(ReviewResponse.from(review))
    }
}
