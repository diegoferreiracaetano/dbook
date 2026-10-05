package com.dbook.application.notification.processnotificationeventusecase

import com.dbook.domain.notification.NotificationChannel
import com.dbook.domain.notification.NotificationPreference
import com.dbook.domain.notification.NotificationType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AChannelSwitchedOffByTheCustomerIsSkippedTest : ProcessNotificationEventFixture() {
    @Test
    fun `given the e-mail switched off for this type when it is processed then only the e-mail is skipped`() {
        preferences.save(
            customerId,
            listOf(NotificationPreference(NotificationType.BOOKING_CONFIRMED, NotificationChannel.EMAIL, false)),
        )

        useCase.execute(confirmedEvent())

        assertTrue(emails.sent.isEmpty())
        assertEquals(1, notifications.all.size)
        assertEquals(1.0, counted("email", "skipped"))
    }

    @Test
    fun `given the e-mail switched off for another type when it is processed then the e-mail still goes out`() {
        preferences.save(
            customerId,
            listOf(NotificationPreference(NotificationType.REFUND_COMPLETED, NotificationChannel.EMAIL, false)),
        )

        useCase.execute(confirmedEvent())

        assertEquals(1, emails.sent.size)
    }
}
