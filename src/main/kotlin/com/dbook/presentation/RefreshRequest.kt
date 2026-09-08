package com.dbook.presentation

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

// single-parameter data class: needs an explicit creator, see RegisterBookingRequest
data class RefreshRequest
    @JsonCreator
    constructor(
        @JsonProperty("refreshToken")
        val refreshToken: String,
    )
