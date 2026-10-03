package com.dbook.presentation.identity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

// single-parameter data class: needs an explicit creator, see SuggestFlightsRequest
data class RefreshRequest
    @JsonCreator
    constructor(
        @JsonProperty("refreshToken")
        val refreshToken: String,
    )
