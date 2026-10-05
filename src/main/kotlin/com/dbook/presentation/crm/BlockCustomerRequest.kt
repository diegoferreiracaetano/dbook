package com.dbook.presentation.crm

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

// single-parameter data class: needs an explicit creator, see RefreshRequest
data class BlockCustomerRequest
    @JsonCreator
    constructor(
        @get:Schema(example = "Chargeback fraud confirmed", description = "at least 10 characters")
        @JsonProperty("reason")
        val reason: String,
    )
