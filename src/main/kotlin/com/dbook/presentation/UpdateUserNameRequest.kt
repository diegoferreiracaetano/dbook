package com.dbook.presentation

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

// single-parameter data class: needs an explicit creator, see RefreshRequest/SuggestFlightsRequest
data class UpdateUserNameRequest
    @JsonCreator
    constructor(
        @get:Schema(example = "Diego Ferreira")
        @JsonProperty("name")
        val name: String,
    )
