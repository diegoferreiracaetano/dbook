package com.dbook.presentation.identity

import com.dbook.domain.identity.UserNotFoundException
import com.dbook.domain.identity.UserRepository
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `GET /admin/auth/me` — the portal builds its menu and routes from the permissions returned here. */
@RestController
@RequestMapping("${ApiPaths.V1}/admin/auth")
@Tag(name = "Admin auth")
@SecurityRequirement(name = "bearerAuth")
class AdminProfileController(
    private val userRepository: UserRepository,
) {
    @Operation(summary = "Returns the staff member's profile, role and permissions")
    @PreAuthorize("hasAuthority('ADMIN_PORTAL_ACCESS')")
    @GetMapping("/me")
    fun me(authentication: Authentication): AdminProfileResponse {
        val user =
            userRepository.findById(authentication.currentUserId())
                ?: throw UserNotFoundException(authentication.currentUserId())
        return AdminProfileResponse.from(user)
    }
}
