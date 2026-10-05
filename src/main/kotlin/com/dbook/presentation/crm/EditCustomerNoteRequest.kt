package com.dbook.presentation.crm

import io.swagger.v3.oas.annotations.media.Schema

data class EditCustomerNoteRequest(
    @get:Schema(example = "Refund approved", description = "omit to keep the text")
    val body: String? = null,
    @get:Schema(example = "true", description = "omit to keep the pin")
    val pinned: Boolean? = null,
)
