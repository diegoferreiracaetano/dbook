package com.dbook.presentation.identity

import io.swagger.v3.oas.annotations.media.Schema

data class ChangePasswordRequest(
    @get:Schema(example = "the-current-passphrase")
    val currentPassword: String,
    @get:Schema(example = "a-new-long-passphrase")
    val newPassword: String,
)
