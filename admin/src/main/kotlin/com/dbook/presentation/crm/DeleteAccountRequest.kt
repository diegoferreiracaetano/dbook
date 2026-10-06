package com.dbook.presentation.crm

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

// single-parameter data class: needs an explicit creator, see RefreshRequest
data class DeleteAccountRequest
    @JsonCreator
    constructor(
        @get:Schema(example = "s3cret-password", description = "the account's current password")
        @JsonProperty("password")
        val password: String,
    )
