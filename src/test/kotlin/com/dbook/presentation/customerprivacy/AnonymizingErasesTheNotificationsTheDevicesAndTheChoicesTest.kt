package com.dbook.presentation.customerprivacy

import com.dbook.application.notification.NotificationEvent
import com.dbook.application.notification.ProcessNotificationEventUseCase
import com.dbook.domain.notification.NotificationType
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class AnonymizingErasesTheNotificationsTheDevicesAndTheChoicesTest : CustomerPrivacyFixture() {
    @Autowired
    lateinit var processNotificationEventUseCase: ProcessNotificationEventUseCase

    @Test
    fun `given a customer with notifications, a device and a choice when anonymized then none of it is left`() {
        val email = uniqueEmail()
        registerAndLogin(email)
        val id = userIdOf(email)
        processNotificationEventUseCase.execute(
            NotificationEvent(
                UUID.randomUUID(),
                NotificationType.BOOKING_CONFIRMED,
                mapOf("customerId" to id, "title" to "GRU-GIG"),
            ),
        )
        jdbcTemplate.update(
            "INSERT INTO device_token (user_id, token, platform, last_seen_at) VALUES (?, ?, 'WEB', now())",
            id,
            "device-$id",
        )
        jdbcTemplate.update(
            "INSERT INTO notification_preference (user_id, type, channel, enabled) " +
                "VALUES (?, 'BOOKING_EXPIRED', 'EMAIL', false)",
            id,
        )
        assertEquals(1, countOf("notification", "user_id", id))

        val result = anonymize(superAdminToken(), id)

        assertEquals(200, result.response.status)
        assertEquals(0, countOf("notification", "user_id", id))
        assertEquals(0, countOf("device_token", "user_id", id))
        assertEquals(0, countOf("notification_preference", "user_id", id))
    }
}
