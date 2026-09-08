package com.dbook.presentation

import com.dbook.application.TokenPair

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
) {
    companion object {
        fun from(tokens: TokenPair) = TokenResponse(tokens.accessToken, tokens.refreshToken)
    }
}
