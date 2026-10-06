package com.dbook.domain.notification

import java.time.Instant

enum class DevicePlatform { ANDROID, IOS, WEB }

data class DeviceToken(
    val userId: Long,
    val token: String,
    val platform: DevicePlatform,
    val lastSeenAt: Instant,
) {
    init {
        require(
            token.isNotBlank() && token.length <= MAX_TOKEN_LENGTH,
        ) { "token must have 1 to $MAX_TOKEN_LENGTH characters" }
    }

    private companion object {
        const val MAX_TOKEN_LENGTH = 255
    }
}

interface DeviceTokenRepository {
    /** A token belongs to one user: registering it again, even for someone else, moves it. */
    fun register(deviceToken: DeviceToken)

    fun remove(
        userId: Long,
        token: String,
    )

    fun findByUser(userId: Long): List<DeviceToken>
}
