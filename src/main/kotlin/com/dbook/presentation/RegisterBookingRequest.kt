package com.dbook.presentation

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

// A single-parameter Kotlin data class trips up Jackson's creator resolution (it can't
// tell a one-property object apart from a delegating wrapper) without an explicit
// @JsonCreator/@JsonProperty pair — Kotlin data classes with 2+ properties don't need this.
data class RegisterBookingRequest
    @JsonCreator
    constructor(
        @get:Schema(
            example = "1",
            description = "id of an existing Bookable (e.g. a flight registered via POST /admin/flights)",
        )
        @JsonProperty("bookableId")
        val bookableId: Long,
    )
