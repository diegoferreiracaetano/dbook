package com.dbook.presentation.identity

// no refresh token here: it only travels in the httpOnly cookie
data class AdminSessionResponse(
    val accessToken: String,
)
