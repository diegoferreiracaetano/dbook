package com.dbook.application.identity

import com.dbook.domain.identity.StaffInvitationRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import java.time.Clock

@Observed(name = "dbook.usecase")
@Service
class ListInvitationsUseCase(
    private val invitations: StaffInvitationRepository,
    private val clock: Clock,
) {
    fun execute(): List<InvitationView> {
        val now = clock.instant()
        return invitations.findRecent().map { InvitationView(it, it.statusAt(now)) }
    }
}
