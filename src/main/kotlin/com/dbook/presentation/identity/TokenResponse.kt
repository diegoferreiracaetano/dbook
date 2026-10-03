package com.dbook.presentation.identity

import com.dbook.application.identity.TokenPair

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
) {
    companion object {
        fun from(tokens: TokenPair) = TokenResponse(tokens.accessToken, tokens.refreshToken)
    }
}
