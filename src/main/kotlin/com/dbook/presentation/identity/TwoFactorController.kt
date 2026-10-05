package com.dbook.presentation.identity

import com.dbook.application.identity.ConfirmTotpUseCase
import com.dbook.application.identity.DisableTwoFactorCommand
import com.dbook.application.identity.DisableTwoFactorUseCase
import com.dbook.application.identity.EnrollTotpUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.currentActor
import com.dbook.presentation.common.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** `/admin/2fa`: a signed-in staff member turning the second factor on or off for their own account. */
@RestController
@RequestMapping("${ApiPaths.V1}/admin/2fa")
@Tag(name = "Admin two-factor", description = "Authenticator app (TOTP) for the staff's own account")
@SecurityRequirement(name = "bearerAuth")
class TwoFactorController(
    private val enrollUseCase: EnrollTotpUseCase,
    private val confirmUseCase: ConfirmTotpUseCase,
    private val disableUseCase: DisableTwoFactorUseCase,
) {
    @Operation(summary = "Starts the enrollment: the secret to add to the authenticator (not active until confirmed)")
    @PreAuthorize("hasAuthority('ADMIN_PORTAL_ACCESS')")
    @PostMapping("/enroll")
    fun enroll(authentication: Authentication): TwoFactorEnrollmentResponse =
        TwoFactorEnrollmentResponse.from(enrollUseCase.execute(authentication.currentUserId()))

    @Operation(summary = "Confirms the enrollment with the first code and shows the recovery codes, once")
    @PreAuthorize("hasAuthority('ADMIN_PORTAL_ACCESS')")
    @PostMapping("/confirm")
    fun confirm(
        @RequestBody request: TwoFactorCodeRequest,
        authentication: Authentication,
    ): RecoveryCodesResponse =
        RecoveryCodesResponse(confirmUseCase.execute(authentication.currentActor(), request.code))

    @Operation(summary = "Turns the second factor off (password and a code; refused where the role requires it)")
    @PreAuthorize("hasAuthority('ADMIN_PORTAL_ACCESS')")
    @PostMapping("/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun disable(
        @RequestBody request: DisableTwoFactorRequest,
        authentication: Authentication,
        servletRequest: HttpServletRequest,
    ) {
        disableUseCase.execute(
            DisableTwoFactorCommand(
                authentication.currentActor(),
                request.password,
                request.code,
                servletRequest.remoteAddr,
            ),
        )
    }
}
