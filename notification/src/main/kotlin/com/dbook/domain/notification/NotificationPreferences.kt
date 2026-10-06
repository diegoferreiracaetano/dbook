package com.dbook.domain.notification

data class NotificationPreference(
    val type: NotificationType,
    val channel: NotificationChannel,
    val enabled: Boolean,
)

interface NotificationPreferenceRepository {
    /** What the user chose, nothing else: a missing pair is enabled. */
    fun findChosen(userId: Long): List<NotificationPreference>

    fun save(
        userId: Long,
        preferences: List<NotificationPreference>,
    )
}

/** The effective preference for every type and channel: the user's choice, or enabled when they made none. */
fun effectivePreferences(chosen: List<NotificationPreference>): List<NotificationPreference> {
    val byPair = chosen.associateBy { it.type to it.channel }
    return NotificationType.entries.flatMap { type ->
        NotificationChannel.entries.map { channel ->
            byPair[type to channel] ?: NotificationPreference(type, channel, enabled = true)
        }
    }
}
