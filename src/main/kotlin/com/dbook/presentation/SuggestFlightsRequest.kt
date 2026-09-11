package com.dbook.presentation

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

// Single-parameter Kotlin data class — Jackson's creator resolution can't tell a
// one-property object apart from a delegating wrapper without this, unlike multi-field DTOs.
data class SuggestFlightsRequest
    @JsonCreator
    constructor(
        @get:Schema(example = "voos baratos pra o Rio mês que vem")
        @JsonProperty("query")
        val query: String,
    )
