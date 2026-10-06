package com.dbook.presentation.promo

import com.dbook.application.promo.ValidatePromoCommand
import com.dbook.application.promo.ValidatePromoUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `POST /promo-codes/validate` — authenticated; what a code would do to the caller's own PENDING bookings. */
@RestController
@RequestMapping("${ApiPaths.V1}/promo-codes")
@Tag(name = "Promo codes", description = "Previewing a promotional code before paying")
@SecurityRequirement(name = "bearerAuth")
class PromoController(
    private val validatePromoUseCase: ValidatePromoUseCase,
) {
    @Operation(
        summary = "Previews the discount of a code on the given bookings, without using the code up",
        description = "404 for an unknown code; 422 PROMO_REJECTED when it cannot be used (and why, in the message).",
    )
    @PostMapping("/validate")
    fun validate(
        @RequestBody request: ValidatePromoRequest,
        authentication: Authentication,
    ): PromoPreviewResponse =
        PromoPreviewResponse.from(
            validatePromoUseCase.execute(
                ValidatePromoCommand(authentication.currentUserId(), request.code, request.bookingIds),
            ),
        )
}
