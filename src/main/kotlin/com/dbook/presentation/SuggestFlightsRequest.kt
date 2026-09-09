package com.dbook.presentation

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

// Single-parameter Kotlin data class — see RegisterBookingRequest for why @JsonCreator
// is needed here but not on multi-field DTOs.
data class SuggestFlightsRequest
    @JsonCreator
    constructor(
        @get:Schema(example = "voos baratos pra o Rio mês que vem")
        @JsonProperty("query")
        val query: String,
    )
