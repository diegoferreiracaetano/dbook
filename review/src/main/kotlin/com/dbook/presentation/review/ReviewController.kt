package com.dbook.presentation.review

import com.dbook.application.review.CreateReviewCommand
import com.dbook.application.review.CreateReviewUseCase
import com.dbook.application.review.DeleteReviewUseCase
import com.dbook.application.review.EditReviewCommand
import com.dbook.application.review.EditReviewUseCase
import com.dbook.application.review.ReportReviewCommand
import com.dbook.application.review.ReportReviewUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** `/reviews` — authenticated; rate your own CONFIRMED bookings, and change, delete or report reviews. */
@RestController
@RequestMapping("${ApiPaths.V1}/reviews")
@Tag(name = "Reviews", description = "rates a trip; edits, deletes or reports a review")
@SecurityRequirement(name = "bearerAuth")
class ReviewController(
    private val createReviewUseCase: CreateReviewUseCase,
    private val editReviewUseCase: EditReviewUseCase,
    private val deleteReviewUseCase: DeleteReviewUseCase,
    private val reportReviewUseCase: ReportReviewUseCase,
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

    @Operation(summary = "changes the rating, the comment or both of the caller's own review (403 if it is not theirs)")
    @PatchMapping("/{id}")
    fun edit(
        @PathVariable id: Long,
        @RequestBody request: EditReviewRequest,
        authentication: Authentication,
    ): ReviewResponse =
        ReviewResponse.from(
            editReviewUseCase.execute(
                EditReviewCommand(id, authentication.currentUserId(), request.rating, request.comment),
            ),
        )

    @Operation(summary = "deletes the caller's own review for good (403 if it is not theirs)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: Long,
        authentication: Authentication,
    ) = deleteReviewUseCase.execute(id, authentication.currentUserId())

    @Operation(summary = "flags a review for the team (once per customer; not your own; 404 for a hidden one)")
    @PostMapping("/{id}/report")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun report(
        @PathVariable id: Long,
        @RequestBody request: ReportReviewRequest,
        authentication: Authentication,
    ) = reportReviewUseCase.execute(ReportReviewCommand(id, authentication.currentUserId(), request.reason))
}
