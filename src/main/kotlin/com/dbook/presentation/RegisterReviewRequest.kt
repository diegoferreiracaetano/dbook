package com.dbook.presentation

import io.swagger.v3.oas.annotations.media.Schema

data class RegisterReviewRequest(
    @get:Schema(example = "1", description = "id of the caller's own CONFIRMED booking being reviewed")
    val bookingId: Long,
    @get:Schema(example = "5", description = "rating from 1 to 5")
    val rating: Int,
    @get:Schema(example = "Great flight!", description = "optional comment")
    val comment: String? = null,
)
