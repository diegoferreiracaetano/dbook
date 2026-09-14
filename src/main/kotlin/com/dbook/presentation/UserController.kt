package com.dbook.presentation

import com.dbook.application.UpdateUserNameCommand
import com.dbook.application.UpdateUserNameUseCase
import com.dbook.domain.UserNotFoundException
import com.dbook.domain.UserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `GET /users/me`, `PATCH /users/me` — authenticated; always the caller's own profile. */
@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "The authenticated user's own profile")
@SecurityRequirement(name = "bearerAuth")
class UserController(
    private val userRepository: UserRepository,
    private val updateUserNameUseCase: UpdateUserNameUseCase,
) {
    @Operation(summary = "Returns the authenticated user's profile")
    @GetMapping("/me")
    fun me(authentication: Authentication): UserResponse {
        val user =
            userRepository.findById(authentication.currentUserId())
                ?: throw UserNotFoundException(authentication.currentUserId())
        return UserResponse.from(user)
    }

    @Operation(summary = "Updates the authenticated user's display name")
    @PatchMapping("/me")
    fun updateName(
        @RequestBody request: UpdateUserNameRequest,
        authentication: Authentication,
    ): UserResponse {
        val user =
            updateUserNameUseCase.execute(
                UpdateUserNameCommand(userId = authentication.currentUserId(), name = request.name),
            )
        return UserResponse.from(user)
    }
}
