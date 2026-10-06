package com.dbook.presentation.identity

import io.swagger.v3.oas.annotations.media.Schema

data class AcceptInvitationRequest(
    @get:Schema(example = "Zq3Yp0hWl5r8t1b2nXy9u4VwLk7oPaSd6EfGcRjIhMs")
    val token: String,
    @get:Schema(example = "Maria Souza")
    val name: String,
    @get:Schema(example = "a-long-passphrase-of-12+")
    val password: String,
)
