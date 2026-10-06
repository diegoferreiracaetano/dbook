package com.dbook.presentation.identity

import com.dbook.domain.common.access.Permission
import com.dbook.domain.common.access.Role
import com.dbook.domain.identity.User

data class AdminProfileResponse(
    val id: Long?,
    val name: String,
    val email: String,
    val role: Role,
    val permissions: List<Permission>,
    val twoFactorEnabled: Boolean,
    val twoFactorRequired: Boolean,
) {
    companion object {
        fun from(
            user: User,
            twoFactorEnabled: Boolean,
            twoFactorRequired: Boolean,
        ) = AdminProfileResponse(
            id = user.id,
            name = user.name,
            email = user.email,
            role = user.role,
            permissions = user.role.permissions.sorted(),
            twoFactorEnabled = twoFactorEnabled,
            twoFactorRequired = twoFactorRequired,
        )
    }
}
