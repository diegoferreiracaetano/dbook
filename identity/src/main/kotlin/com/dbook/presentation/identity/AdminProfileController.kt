package com.dbook.presentation.identity

import com.dbook.application.identity.ChangePasswordCommand
import com.dbook.application.identity.ChangePasswordUseCase
import com.dbook.application.identity.TwoFactorGate
import com.dbook.domain.identity.UserNotFoundException
import com.dbook.domain.identity.UserRepository
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** `GET /admin/auth/me` (the portal builds its menu from it) and `POST /admin/auth/change-password`. */
@RestController
@RequestMapping("${ApiPaths.V1}/admin/auth")
@Tag(name = "Admin auth")
@SecurityRequirement(name = "bearerAuth")
class AdminProfileController(
    private val userRepository: UserRepository,
    private val twoFactorGate: TwoFactorGate,
    private val changePasswordUseCase: ChangePasswordUseCase,
) {
    @Operation(summary = "Returns the staff member's profile, role and permissions")
    @PreAuthorize("hasAuthority('ADMIN_PORTAL_ACCESS')")
    @GetMapping("/me")
    fun me(authentication: Authentication): AdminProfileResponse {
        val user =
            userRepository.findById(authentication.currentUserId())
                ?: throw UserNotFoundException(authentication.currentUserId())
        return AdminProfileResponse.from(
            user,
            twoFactorEnabled = twoFactorGate.isEnabled(authentication.currentUserId()),
            twoFactorRequired = twoFactorGate.isRequiredFor(user.role),
        )
    }

    @Operation(summary = "Changes the staff member's own password and ends every session (log in again afterwards)")
    @PreAuthorize("hasAuthority('ADMIN_PORTAL_ACCESS')")
    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changePassword(
        @RequestBody request: ChangePasswordRequest,
        authentication: Authentication,
        servletRequest: HttpServletRequest,
    ) {
        changePasswordUseCase.execute(
            ChangePasswordCommand(
                authentication.currentUserId(),
                request.currentPassword,
                request.newPassword,
                servletRequest.remoteAddr,
            ),
        )
    }
}
