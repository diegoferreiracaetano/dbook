package com.dbook.presentation.identity

import com.dbook.application.identity.InvitationView
import com.dbook.domain.common.access.Role
import com.dbook.domain.identity.InvitationStatus
import java.time.Instant

/** Never carries the token (it only exists in the e-mail) nor its hash. */
data class StaffInvitationResponse(
    val id: Long?,
    val email: String,
    val role: Role,
    val status: InvitationStatus,
    val invitedBy: Long,
    val createdAt: Instant,
    val expiresAt: Instant,
) {
    companion object {
        fun from(view: InvitationView) =
            StaffInvitationResponse(
                id = view.invitation.id,
                email = view.invitation.email,
                role = view.invitation.role,
                status = view.status,
                invitedBy = view.invitation.invitedBy,
                createdAt = view.invitation.createdAt,
                expiresAt = view.invitation.expiresAt,
            )
    }
}
