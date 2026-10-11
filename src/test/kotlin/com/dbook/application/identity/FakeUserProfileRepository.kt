package com.dbook.application.identity

import com.dbook.domain.identity.UserProfile
import com.dbook.domain.identity.UserProfileRepository

class FakeUserProfileRepository : UserProfileRepository {
    val stored = mutableMapOf<Long, UserProfile>()

    override fun find(userId: Long): UserProfile? = stored[userId]

    override fun save(profile: UserProfile): UserProfile {
        stored[profile.userId] = profile
        return profile
    }
}
