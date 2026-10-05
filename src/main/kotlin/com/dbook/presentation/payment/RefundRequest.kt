package com.dbook.presentation.payment

import com.dbook.domain.payment.RefundReason
import io.swagger.v3.oas.annotations.media.Schema

data class RefundRequest(
    @get:Schema(example = "CUSTOMER_REQUEST")
    val reason: RefundReason,
    @get:Schema(
        example = "Customer called to cancel the trip",
        description = "up to 500 characters; required with override",
    )
    val note: String? = null,
    @get:Schema(
        example = "false",
        description = "refund inside the last 24 hours before departure: SUPER_ADMIN only, with a note",
    )
    val override: Boolean = false,
)
