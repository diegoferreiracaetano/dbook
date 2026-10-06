package com.dbook.presentation.crm

import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserStatus
import java.time.Instant

data class CustomerAccountResponse(
    val id: Long?,
    val status: UserStatus,
    val blockedReason: String?,
    val blockedAt: Instant?,
) {
    companion object {
        fun from(user: User) = CustomerAccountResponse(user.id, user.status, user.blockedReason, user.blockedAt)
    }
}
