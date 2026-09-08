package com.dbook.presentation

import io.swagger.v3.oas.annotations.media.Schema

data class LoginRequest(
    @get:Schema(example = "diego@example.com")
    val email: String,
    @get:Schema(example = "s3cret-password")
    val password: String,
)
