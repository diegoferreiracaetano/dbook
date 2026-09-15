package com.dbook.presentation

import com.dbook.application.RegisterPaymentCommand
import com.dbook.application.RegisterPaymentUseCase
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

/** `POST /payments` — authenticated; pays for one or more of the caller's own PENDING bookings at once. */
@RestController
@RequestMapping("/payments")
@Tag(name = "Payments", description = "Payment and confirmation of PENDING bookings")
@SecurityRequirement(name = "bearerAuth")
class PaymentController(
    private val registerPaymentUseCase: RegisterPaymentUseCase,
) {
    @Operation(summary = "Pays for the given bookings, confirming each of them")
    @PostMapping
    fun register(
        @RequestBody request: RegisterPaymentRequest,
        authentication: Authentication,
    ): ResponseEntity<PaymentResponse> {
        val payment =
            registerPaymentUseCase.execute(
                RegisterPaymentCommand(
                    bookingIds = request.bookingIds,
                    cardLast4 = request.cardLast4,
                    cardholderName = request.cardholderName,
                    requestingUserId = authentication.currentUserId(),
                ),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(payment, request.bookingIds))
    }
}
