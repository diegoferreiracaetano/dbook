package com.dbook.presentation.crm

import io.swagger.v3.oas.annotations.media.Schema

data class AnonymizeCustomerRequest(
    @get:Schema(example = "Erasure requested by the customer by e-mail", description = "at least 10 characters")
    val reason: String,
    @get:Schema(example = "ANONYMIZE 42", description = "the word ANONYMIZE and the customer id, exactly")
    val confirmation: String,
)
