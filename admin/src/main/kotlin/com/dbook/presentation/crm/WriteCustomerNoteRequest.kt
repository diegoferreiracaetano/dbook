package com.dbook.presentation.crm

import io.swagger.v3.oas.annotations.media.Schema

data class WriteCustomerNoteRequest(
    @get:Schema(example = "Asked for a refund by phone; waiting for the airline", description = "up to 2000 characters")
    val body: String,
    @get:Schema(example = "false", description = "pinned notes come first")
    val pinned: Boolean = false,
)
