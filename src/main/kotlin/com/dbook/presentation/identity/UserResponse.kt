package com.dbook.presentation.identity

import com.dbook.domain.identity.Role
import com.dbook.domain.identity.User

data class UserResponse(
    val id: Long?,
    val email: String,
    val name: String,
    val role: Role,
) {
    companion object {
        fun from(user: User) = UserResponse(id = user.id, email = user.email, name = user.name, role = user.role)
    }
}
