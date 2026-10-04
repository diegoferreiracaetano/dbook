package com.dbook.presentation.identity

import com.dbook.application.identity.InviteStaffCommand
import com.dbook.application.identity.InviteStaffUseCase
import com.dbook.application.identity.ListInvitationsUseCase
import com.dbook.application.identity.ResendInvitationCommand
import com.dbook.application.identity.ResendInvitationUseCase
import com.dbook.application.identity.RevokeInvitationCommand
import com.dbook.application.identity.RevokeInvitationUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.currentActor
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** `/admin/invitations` — inviting people to the staff; all of it needs ADMIN_MANAGE. */
@RestController
@RequestMapping("${ApiPaths.V1}/admin/invitations")
@Tag(name = "Staff invitations (admin)", description = "Inviting people to join the staff")
@SecurityRequirement(name = "bearerAuth")
class StaffInvitationController(
    private val inviteStaffUseCase: InviteStaffUseCase,
    private val resendInvitationUseCase: ResendInvitationUseCase,
    private val revokeInvitationUseCase: RevokeInvitationUseCase,
    private val listInvitationsUseCase: ListInvitationsUseCase,
) {
    @Operation(summary = "Invites someone to the staff by e-mail; inviting again replaces the open invitation")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE')")
    @PostMapping
    fun invite(
        @RequestBody request: InviteStaffRequest,
        authentication: Authentication,
    ): ResponseEntity<StaffInvitationResponse> {
        val view =
            inviteStaffUseCase.execute(
                InviteStaffCommand(authentication.currentActor(), request.email, request.role),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(StaffInvitationResponse.from(view))
    }

    @Operation(summary = "Lists the most recent invitations with their status, newest first")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE')")
    @GetMapping
    fun list(): List<StaffInvitationResponse> = listInvitationsUseCase.execute().map(StaffInvitationResponse::from)

    @Operation(summary = "Sends an open invitation again, with a new link")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE')")
    @PostMapping("/{id}/resend")
    fun resend(
        @PathVariable id: Long,
        authentication: Authentication,
    ): StaffInvitationResponse =
        StaffInvitationResponse.from(
            resendInvitationUseCase.execute(ResendInvitationCommand(authentication.currentActor(), id)),
        )

    @Operation(summary = "Revokes an open invitation: its link stops working")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revoke(
        @PathVariable id: Long,
        authentication: Authentication,
    ) {
        revokeInvitationUseCase.execute(RevokeInvitationCommand(authentication.currentActor(), id))
    }
}
