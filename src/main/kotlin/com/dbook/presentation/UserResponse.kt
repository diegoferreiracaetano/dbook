package com.dbook.presentation

import com.dbook.domain.Role
import com.dbook.domain.User

data class UserResponse(
    val id: Long?,
    val email: String,
    val role: Role,
) {
    companion object {
        fun from(user: User) = UserResponse(id = user.id, email = user.email, role = user.role)
    }
}
