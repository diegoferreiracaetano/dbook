package com.dbook.presentation.review

import com.dbook.application.review.DismissReviewReportsUseCase
import com.dbook.application.review.HideReviewCommand
import com.dbook.application.review.HideReviewUseCase
import com.dbook.application.review.RestoreReviewUseCase
import com.dbook.application.review.SearchAdminReviewsUseCase
import com.dbook.domain.review.AdminReviewStatus
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.PageParams
import com.dbook.presentation.common.PageResponse
import com.dbook.presentation.common.currentActor
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** `/admin/reviews` — the moderation queue and what the team does with a review. */
@RestController
@RequestMapping("${ApiPaths.V1}/admin/reviews")
@Tag(name = "Reviews (admin)", description = "Moderation: the reported reviews, hiding and restoring")
@SecurityRequirement(name = "bearerAuth")
class AdminReviewController(
    private val searchAdminReviewsUseCase: SearchAdminReviewsUseCase,
    private val hideReviewUseCase: HideReviewUseCase,
    private val restoreReviewUseCase: RestoreReviewUseCase,
    private val dismissReviewReportsUseCase: DismissReviewReportsUseCase,
) {
    @Operation(summary = "Reviews by status (REPORTED is the queue: visible, with an open report) (REVIEW_MODERATE)")
    @PreAuthorize("hasAuthority('REVIEW_MODERATE')")
    @GetMapping
    fun search(
        @RequestParam(required = false) status: AdminReviewStatus?,
        params: PageParams,
    ): PageResponse<AdminReviewResponse> =
        PageResponse.from(searchAdminReviewsUseCase.execute(status, params.toQuery()), AdminReviewResponse::from)

    @Operation(
        summary = "Hides a review, with the reason: it stops showing and counting (REVIEW_MODERATE)",
    )
    @PreAuthorize("hasAuthority('REVIEW_MODERATE')")
    @PostMapping("/{id}/hide")
    fun hide(
        @PathVariable id: Long,
        @RequestBody request: HideReviewRequest,
        authentication: Authentication,
    ): ReviewResponse =
        ReviewResponse.from(
            hideReviewUseCase.execute(HideReviewCommand(authentication.currentActor(), id, request.reason)),
        )

    @Operation(summary = "Puts a hidden review back (REVIEW_MODERATE)")
    @PreAuthorize("hasAuthority('REVIEW_MODERATE')")
    @PostMapping("/{id}/restore")
    fun restore(
        @PathVariable id: Long,
        authentication: Authentication,
    ): ReviewResponse = ReviewResponse.from(restoreReviewUseCase.execute(authentication.currentActor(), id))

    @Operation(summary = "Closes the open reports of a review that stays (REVIEW_MODERATE)")
    @PreAuthorize("hasAuthority('REVIEW_MODERATE')")
    @PostMapping("/{id}/dismiss-reports")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun dismissReports(
        @PathVariable id: Long,
        authentication: Authentication,
    ) {
        dismissReviewReportsUseCase.execute(authentication.currentActor(), id)
    }
}
