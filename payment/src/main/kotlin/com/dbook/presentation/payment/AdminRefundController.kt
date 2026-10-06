package com.dbook.presentation.payment

import com.dbook.application.payment.ListRefundsUseCase
import com.dbook.application.payment.RefundBookingUseCase
import com.dbook.application.payment.RefundCommand
import com.dbook.application.payment.RetryRefundUseCase
import com.dbook.domain.payment.RefundStatus
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.PageParams
import com.dbook.presentation.common.PageResponse
import com.dbook.presentation.common.currentActor
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** Refunding a booking and following the refunds up. All of it needs PAYMENT_REFUND. */
@RestController
@RequestMapping("${ApiPaths.V1}/admin")
@Tag(name = "Refunds (admin)", description = "Giving the money of a booking back")
@SecurityRequirement(name = "bearerAuth")
class AdminRefundController(
    private val refundBookingUseCase: RefundBookingUseCase,
    private val retryRefundUseCase: RetryRefundUseCase,
    private val listRefundsUseCase: ListRefundsUseCase,
) {
    @Operation(
        summary = "Refunds a CONFIRMED booking in full; idempotent by the Idempotency-Key header",
        description =
            "Returns the refund as it ended: COMPLETED, or FAILED when the payment gateway refused (the money " +
                "did not move; retry it). Inside the last 24 hours before departure only a SUPER_ADMIN can, with " +
                "`override` and a note.",
    )
    @PreAuthorize("hasAuthority('PAYMENT_REFUND')")
    @PostMapping("/bookings/{id}/refund")
    fun refund(
        @PathVariable id: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestBody request: RefundRequest,
        authentication: Authentication,
    ): ResponseEntity<RefundResponse> {
        val command =
            RefundCommand(
                authentication.currentActor(),
                id,
                request.reason,
                request.note,
                request.override,
                idempotencyKey,
            )
        return ResponseEntity.status(
            HttpStatus.CREATED,
        ).body(RefundResponse.from(refundBookingUseCase.execute(command)))
    }

    @Operation(summary = "Lists refunds, newest first, optionally only one status (to find the FAILED ones)")
    @PreAuthorize("hasAuthority('PAYMENT_REFUND')")
    @GetMapping("/refunds")
    fun list(
        @RequestParam(required = false) status: RefundStatus?,
        params: PageParams,
    ): PageResponse<RefundResponse> =
        PageResponse.from(listRefundsUseCase.execute(status, params.toQuery()), RefundResponse::from)

    @Operation(summary = "One refund")
    @PreAuthorize("hasAuthority('PAYMENT_REFUND')")
    @GetMapping("/refunds/{id}")
    fun get(
        @PathVariable id: Long,
    ): RefundResponse = RefundResponse.from(listRefundsUseCase.find(id))

    @Operation(summary = "Tries a FAILED refund again (safe to repeat: the gateway is asked with the refund's own key)")
    @PreAuthorize("hasAuthority('PAYMENT_REFUND')")
    @PostMapping("/refunds/{id}/retry")
    fun retry(
        @PathVariable id: Long,
        authentication: Authentication,
    ): RefundResponse = RefundResponse.from(retryRefundUseCase.execute(id, authentication.currentActor()))
}
