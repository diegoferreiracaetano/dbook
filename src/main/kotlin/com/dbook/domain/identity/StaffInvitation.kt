package com.dbook.domain.identity

import com.dbook.domain.common.access.Role
import java.time.Duration
import java.time.Instant

class StaffInvitation(
    val id: Long? = null,
    val email: String,
    val role: Role,
    val tokenHash: String,
    val invitedBy: Long,
    val expiresAt: Instant,
    val createdAt: Instant,
    val acceptedAt: Instant? = null,
    val revokedAt: Instant? = null,
    val version: Long = 0,
) {
    init {
        require(email.isNotBlank() && email.contains("@")) { "email must be a valid address" }
        require(role.isStaff) { "an invitation is for a staff role" }
        require(expiresAt.isAfter(createdAt)) { "an invitation must expire after it was created" }
    }

    val isOpen: Boolean get() = acceptedAt == null && revokedAt == null

    fun statusAt(now: Instant): InvitationStatus =
        when {
            acceptedAt != null -> InvitationStatus.ACCEPTED
            revokedAt != null -> InvitationStatus.REVOKED
            !expiresAt.isAfter(now) -> InvitationStatus.EXPIRED
            else -> InvitationStatus.PENDING
        }

    fun accept(now: Instant): StaffInvitation {
        check(statusAt(now) == InvitationStatus.PENDING) { "Only a pending invitation can be accepted" }
        return copy(acceptedAt = now)
    }

    // an expired one can be revoked too: it still holds the e-mail's single open slot
    fun revoke(now: Instant): StaffInvitation {
        check(isOpen) { "Only an open invitation can be revoked" }
        return copy(revokedAt = now)
    }

    fun reissue(
        newTokenHash: String,
        now: Instant,
    ): StaffInvitation {
        check(isOpen) { "Only an open invitation can be sent again" }
        return copy(tokenHash = newTokenHash, expiresAt = now.plus(VALIDITY))
    }

    private fun copy(
        tokenHash: String = this.tokenHash,
        expiresAt: Instant = this.expiresAt,
        acceptedAt: Instant? = this.acceptedAt,
        revokedAt: Instant? = this.revokedAt,
    ) = StaffInvitation(id, email, role, tokenHash, invitedBy, expiresAt, createdAt, acceptedAt, revokedAt, version)

    companion object {
        val VALIDITY: Duration = Duration.ofHours(72)

        fun issue(
            email: String,
            role: Role,
            tokenHash: String,
            invitedBy: Long,
            now: Instant,
        ) = StaffInvitation(
            email = email.trim().lowercase(),
            role = role,
            tokenHash = tokenHash,
            invitedBy = invitedBy,
            expiresAt = now.plus(VALIDITY),
            createdAt = now,
        )
    }
}
