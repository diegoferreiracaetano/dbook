package com.dbook.presentation.identity

import com.dbook.domain.common.access.Role
import io.swagger.v3.oas.annotations.media.Schema

data class InviteStaffRequest(
    @get:Schema(example = "maria@example.com")
    val email: String,
    @get:Schema(example = "SUPPORT")
    val role: Role,
)
