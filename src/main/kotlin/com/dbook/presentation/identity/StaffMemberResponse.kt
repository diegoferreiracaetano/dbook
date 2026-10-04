package com.dbook.presentation.identity

import com.dbook.domain.identity.Role
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserStatus
import java.time.Instant

data class StaffMemberResponse(
    val id: Long?,
    val name: String,
    val email: String,
    val role: Role,
    val status: UserStatus,
    val blockedReason: String?,
    val lastLoginAt: Instant?,
) {
    companion object {
        fun from(user: User) =
            StaffMemberResponse(
                id = user.id,
                name = user.name,
                email = user.email,
                role = user.role,
                status = user.status,
                blockedReason = user.blockedReason,
                lastLoginAt = user.lastLoginAt,
            )
    }
}
