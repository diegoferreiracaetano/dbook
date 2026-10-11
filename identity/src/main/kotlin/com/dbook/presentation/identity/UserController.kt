package com.dbook.presentation.identity

import com.dbook.application.identity.ChangePasswordCommand
import com.dbook.application.identity.ChangePasswordUseCase
import com.dbook.application.identity.EndMySessionUseCase
import com.dbook.application.identity.GetUserProfileUseCase
import com.dbook.application.identity.ListMySessionsUseCase
import com.dbook.application.identity.SetUserAvatarUseCase
import com.dbook.application.identity.UpdateUserNameCommand
import com.dbook.application.identity.UpdateUserNameUseCase
import com.dbook.application.identity.UpdateUserPreferencesUseCase
import com.dbook.domain.identity.UserNotFoundException
import com.dbook.domain.identity.UserRepository
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** The `/users/me` endpoints — authenticated; always the caller's own profile, picture, preferences and password. */
@RestController
@RequestMapping("${ApiPaths.V1}/users")
@Tag(name = "Users", description = "The authenticated user's own profile, picture and preferences")
@SecurityRequirement(name = "bearerAuth")
class UserController(
    private val userRepository: UserRepository,
    private val updateUserNameUseCase: UpdateUserNameUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserPreferencesUseCase: UpdateUserPreferencesUseCase,
    private val setUserAvatarUseCase: SetUserAvatarUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val listMySessionsUseCase: ListMySessionsUseCase,
    private val endMySessionUseCase: EndMySessionUseCase,
) {
    @Operation(
        summary = "Returns the authenticated user's profile (with the picture and since when the account exists)",
    )
    @GetMapping("/me")
    fun me(authentication: Authentication): UserResponse {
        val userId = authentication.currentUserId()
        val user = userRepository.findById(userId) ?: throw UserNotFoundException(userId)
        return UserResponse.from(user, getUserProfileUseCase.execute(userId))
    }

    @Operation(summary = "Updates the authenticated user's display name")
    @PatchMapping("/me")
    fun updateName(
        @RequestBody request: UpdateUserNameRequest,
        authentication: Authentication,
    ): UserResponse {
        val userId = authentication.currentUserId()
        val user = updateUserNameUseCase.execute(UpdateUserNameCommand(userId = userId, name = request.name))
        return UserResponse.from(user, getUserProfileUseCase.execute(userId))
    }

    @Operation(summary = "Returns the preferences (language, theme, home airport, currency, ...); null = no choice")
    @GetMapping("/me/preferences")
    fun preferences(authentication: Authentication): PreferencesResponse =
        PreferencesResponse.from(getUserProfileUseCase.execute(authentication.currentUserId()).preferences)

    @Operation(summary = "Replaces the preferences as a whole: a field left out clears that preference")
    @PutMapping("/me/preferences")
    fun updatePreferences(
        @RequestBody request: PreferencesRequest,
        authentication: Authentication,
    ): PreferencesResponse =
        PreferencesResponse.from(
            updateUserPreferencesUseCase.execute(authentication.currentUserId(), request.toPreferences()).preferences,
        )

    @Operation(summary = "Sets the profile picture (an https URL)")
    @PutMapping("/me/avatar")
    fun setAvatar(
        @RequestBody request: AvatarRequest,
        authentication: Authentication,
    ): UserResponse = changeAvatar(authentication, request.avatarUrl)

    @Operation(summary = "Removes the profile picture")
    @DeleteMapping("/me/avatar")
    fun removeAvatar(authentication: Authentication): UserResponse = changeAvatar(authentication, null)

    @Operation(summary = "Changes the caller's own password and ends every session (log in again afterwards)")
    @PostMapping("/me/password")
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

    @Operation(summary = "Lists the devices signed in to the caller's account, most recently used first")
    @GetMapping("/me/sessions")
    fun sessions(authentication: Authentication): List<SessionResponse> =
        listMySessionsUseCase.execute(authentication.currentUserId()).map {
            SessionResponse(it.familyId, it.createdAt, it.expiresAt)
        }

    @Operation(summary = "Ends one of the caller's sessions; another user's or an unknown id is a 404")
    @DeleteMapping("/me/sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun endSession(
        @PathVariable id: String,
        authentication: Authentication,
    ) = endMySessionUseCase.execute(authentication.currentUserId(), id)

    private fun changeAvatar(
        authentication: Authentication,
        avatarUrl: String?,
    ): UserResponse {
        val userId = authentication.currentUserId()
        val profile = setUserAvatarUseCase.execute(userId, avatarUrl)
        val user = userRepository.findById(userId) ?: throw UserNotFoundException(userId)
        return UserResponse.from(user, profile)
    }
}
