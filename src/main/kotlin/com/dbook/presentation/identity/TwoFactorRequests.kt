package com.dbook.presentation.identity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

// the single-field ones need an explicit creator, see RefreshRequest

/** The challenge token of the password step, to start or finish the enrollment that the sign-in demands. */
data class TwoFactorChallengeRequest
    @JsonCreator
    constructor(
        @JsonProperty("challengeToken")
        val challengeToken: String,
    )

/** The challenge token and the code: six digits from the authenticator, or a recovery code. */
data class TwoFactorVerifyRequest(
    val challengeToken: String,
    @get:Schema(example = "123456")
    val code: String,
)

data class TwoFactorCodeRequest
    @JsonCreator
    constructor(
        @get:Schema(example = "123456")
        @JsonProperty("code")
        val code: String,
    )

data class DisableTwoFactorRequest(
    val password: String,
    @get:Schema(example = "123456")
    val code: String,
)

data class ResetTwoFactorRequest
    @JsonCreator
    constructor(
        @get:Schema(example = "lost the phone and the recovery codes")
        @JsonProperty("reason")
        val reason: String,
    )
