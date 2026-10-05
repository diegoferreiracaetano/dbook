package com.dbook.presentation.review

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class EditReviewRequest(
    @get:Schema(example = "4", description = "new rating from 1 to 5; leave out to keep the current one")
    val rating: Int? = null,
    @get:Schema(example = "Good, but the boarding was slow", description = "new comment; leave out to keep it")
    val comment: String? = null,
)

// single-parameter data classes: need an explicit creator, see RefreshRequest
data class ReportReviewRequest
    @JsonCreator
    constructor(
        @get:Schema(example = "Offensive language", description = "3 to 500 characters")
        @JsonProperty("reason")
        val reason: String,
    )

data class HideReviewRequest
    @JsonCreator
    constructor(
        @get:Schema(example = "Offensive language towards the crew", description = "10 to 500 characters")
        @JsonProperty("reason")
        val reason: String,
    )
