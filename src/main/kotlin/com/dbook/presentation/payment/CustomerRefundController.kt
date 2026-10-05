package com.dbook.presentation.payment

import com.dbook.application.payment.GetCancellationPolicyUseCase
import com.dbook.application.payment.RequestOwnRefundUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** What a customer can do about their own booking: see the policy first, then ask for the money back. */
@RestController
@RequestMapping("${ApiPaths.V1}/bookings")
@Tag(name = "Cancellation", description = "The customer's own cancellation policy and refund request")
@SecurityRequirement(name = "bearerAuth")
class CustomerRefundController(
    private val getCancellationPolicyUseCase: GetCancellationPolicyUseCase,
    private val requestOwnRefundUseCase: RequestOwnRefundUseCase,
) {
    @Operation(
        summary = "What the caller can do about this booking, how much they get back and until when",
        description = "403 if the booking is not theirs. Show it before the customer confirms.",
    )
    @GetMapping("/{id}/cancellation-policy")
    fun policy(
        @PathVariable id: Long,
        authentication: Authentication,
    ): CancellationPolicyResponse =
        CancellationPolicyResponse.from(getCancellationPolicyUseCase.execute(authentication.currentUserId(), id))

    @Operation(
        summary = "Asks for the money of the caller's own paid booking back; idempotent by the Idempotency-Key header",
        description =
            "Returns the refund as it ended: COMPLETED, or FAILED when the payment gateway refused (the money did " +
                "not move; ask again with a new key, or ask support). 409 REFUND_WINDOW_CLOSED inside the last 24 " +
                "hours before departure, and 409 if the booking is not paid or already has a refund.",
    )
    @PostMapping("/{id}/refund-request")
    fun refundRequest(
        @PathVariable id: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        authentication: Authentication,
    ): ResponseEntity<RefundResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(
            RefundResponse.from(requestOwnRefundUseCase.execute(authentication.currentUserId(), id, idempotencyKey)),
        )
}
