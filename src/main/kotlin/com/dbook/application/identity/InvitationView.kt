package com.dbook.application.identity

import com.dbook.domain.identity.InvitationStatus
import com.dbook.domain.identity.StaffInvitation

/** An invitation together with its status at the moment it was read (the status is derived from the clock). */
data class InvitationView(
    val invitation: StaffInvitation,
    val status: InvitationStatus,
)
