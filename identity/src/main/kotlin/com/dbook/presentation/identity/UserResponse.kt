package com.dbook.presentation.identity

import com.dbook.domain.common.access.Role
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserProfile
import java.time.Instant

data class UserResponse(
    val id: Long?,
    val email: String,
    val name: String,
    val role: Role,
    val emailVerified: Boolean,
    val createdAt: Instant? = null,
    val avatarUrl: String? = null,
) {
    companion object {
        fun from(
            user: User,
            profile: UserProfile? = null,
        ) = UserResponse(
            id = user.id,
            email = user.email,
            name = user.name,
            role = user.role,
            emailVerified = user.isEmailVerified,
            createdAt = user.createdAt,
            avatarUrl = profile?.avatarUrl,
        )
    }
}
