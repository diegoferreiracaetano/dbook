package com.dbook.presentation.identity

import com.dbook.application.identity.AcceptInvitationCommand
import com.dbook.application.identity.AcceptInvitationUseCase
import com.dbook.application.identity.TwoFactorGate
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.PublicEndpoints
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `POST /admin/invitations/accept` — open, because the invitee has no account yet: the token is the proof. */
@RestController
@RequestMapping("${ApiPaths.V1}/admin/invitations")
@Tag(name = "Staff invitations (admin)", description = "Inviting people to join the staff")
@PublicEndpoints
class AcceptInvitationController(
    private val acceptInvitationUseCase: AcceptInvitationUseCase,
    private val twoFactorGate: TwoFactorGate,
) {
    @Operation(summary = "Accepts an invitation: chooses a name and a password and becomes a staff member")
    @PostMapping("/accept")
    fun accept(
        @RequestBody request: AcceptInvitationRequest,
    ): ResponseEntity<AdminProfileResponse> {
        val user =
            acceptInvitationUseCase.execute(
                AcceptInvitationCommand(request.token, request.name, request.password),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            AdminProfileResponse.from(
                user,
                twoFactorEnabled = false,
                twoFactorRequired = twoFactorGate.isRequiredFor(user.role),
            ),
        )
    }
}
