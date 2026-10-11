package com.dbook.application.identity

import com.dbook.domain.identity.UserNotFoundException
import com.dbook.domain.identity.UserPreferences
import com.dbook.domain.identity.UserProfile
import com.dbook.domain.identity.UserProfileRepository
import com.dbook.domain.identity.UserRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** The caller's picture and preferences; a user who never chose anything gets the empty profile. */
@Observed(name = "dbook.usecase")
@Service
class GetUserProfileUseCase(
    private val profiles: UserProfileRepository,
) {
    fun execute(userId: Long): UserProfile = profiles.find(userId) ?: UserProfile(userId)
}

/** Replaces the caller's preferences as a whole: a field left out is a choice taken back. */
@Observed(name = "dbook.usecase")
@Service
class UpdateUserPreferencesUseCase(
    private val users: UserRepository,
    private val profiles: UserProfileRepository,
) {
    @Transactional
    fun execute(
        userId: Long,
        preferences: UserPreferences,
    ): UserProfile {
        users.findById(userId) ?: throw UserNotFoundException(userId)
        val current = profiles.find(userId) ?: UserProfile(userId)
        return profiles.save(current.withPreferences(preferences))
    }
}

/** Sets (or, with `null`, removes) the caller's picture. */
@Observed(name = "dbook.usecase")
@Service
class SetUserAvatarUseCase(
    private val users: UserRepository,
    private val profiles: UserProfileRepository,
) {
    @Transactional
    fun execute(
        userId: Long,
        avatarUrl: String?,
    ): UserProfile {
        users.findById(userId) ?: throw UserNotFoundException(userId)
        val current = profiles.find(userId) ?: UserProfile(userId)
        return profiles.save(current.withAvatar(avatarUrl?.trim()?.takeIf { it.isNotEmpty() }))
    }
}
