package com.dbook.presentation.identity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

// single-field ones need an explicit creator, see RefreshRequest

data class VerifyEmailRequest
    @JsonCreator
    constructor(
        @get:Schema(description = "The token in the link that was mailed")
        @JsonProperty("token")
        val token: String,
    )

data class ForgotPasswordRequest
    @JsonCreator
    constructor(
        @get:Schema(example = "diego@example.com")
        @JsonProperty("email")
        val email: String,
    )

data class ResetPasswordRequest(
    @get:Schema(description = "The token in the link that was mailed")
    val token: String,
    @get:Schema(example = "a-new-s3cret-password")
    val newPassword: String,
)
