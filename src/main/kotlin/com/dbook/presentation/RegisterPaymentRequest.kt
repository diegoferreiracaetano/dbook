package com.dbook.presentation

import io.swagger.v3.oas.annotations.media.Schema

data class RegisterPaymentRequest(
    @get:Schema(
        example = "[1, 2]",
        description = "ids of the caller's own PENDING bookings this payment covers — every leg of a trip at once",
    )
    val bookingIds: List<Long>,
    @get:Schema(example = "4242", description = "last 4 digits of the card — the full number/CVV are never sent")
    val cardLast4: String,
    @get:Schema(example = "Jane Doe", description = "name on the card")
    val cardholderName: String,
)
