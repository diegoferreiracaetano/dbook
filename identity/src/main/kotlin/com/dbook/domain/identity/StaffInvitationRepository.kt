package com.dbook.domain.identity

interface StaffInvitationRepository {
    fun save(invitation: StaffInvitation): StaffInvitation

    fun findById(id: Long): StaffInvitation?

    fun findByTokenHash(tokenHash: String): StaffInvitation?

    fun findOpenByEmail(email: String): StaffInvitation?

    /** The most recent invitations (a fixed maximum), newest first. */
    fun findRecent(): List<StaffInvitation>
}
