package com.dbook.infrastructure.persistence.identity

import com.dbook.domain.identity.UserPreferences
import com.dbook.domain.identity.UserProfile
import com.dbook.domain.identity.UserProfileRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface UserProfileJpaRepository : JpaRepository<UserProfileJpaEntity, Long>

@Repository
class UserProfileRepositoryAdapter(
    private val jpa: UserProfileJpaRepository,
) : UserProfileRepository {
    override fun find(userId: Long): UserProfile? = jpa.findById(userId).orElse(null)?.toDomain()

    override fun save(profile: UserProfile): UserProfile = jpa.save(profile.toJpaEntity()).toDomain()
}

private fun UserProfileJpaEntity.toDomain() =
    UserProfile(
        userId = userId,
        avatarUrl = avatarUrl,
        preferences =
            UserPreferences(
                language = language,
                theme = theme,
                homeAirport = homeAirport,
                country = country,
                currency = currency,
                cabinClass = cabinClass,
                seatPreference = seatPreference,
                dateFormat = dateFormat,
                distanceUnit = distanceUnit,
            ),
    )

private fun UserProfile.toJpaEntity() =
    UserProfileJpaEntity(
        userId = userId,
        avatarUrl = avatarUrl,
        language = preferences.language,
        theme = preferences.theme,
        homeAirport = preferences.homeAirport,
        country = preferences.country,
        currency = preferences.currency,
        cabinClass = preferences.cabinClass,
        seatPreference = preferences.seatPreference,
        dateFormat = preferences.dateFormat,
        distanceUnit = preferences.distanceUnit,
    )
