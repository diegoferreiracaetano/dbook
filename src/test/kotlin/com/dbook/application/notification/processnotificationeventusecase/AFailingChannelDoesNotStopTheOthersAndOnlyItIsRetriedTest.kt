package com.dbook.application.notification.processnotificationeventusecase

import com.dbook.application.notification.NotificationDeliveryException
import com.dbook.domain.identity.EmailDeliveryException
import com.dbook.domain.notification.NotificationChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AFailingChannelDoesNotStopTheOthersAndOnlyItIsRetriedTest : ProcessNotificationEventFixture() {
    @Test
    fun `given the mail server down when processed then the inbox is written and the message is left to retry`() {
        emails.failWith = EmailDeliveryException("smtp down", RuntimeException())
        val event = confirmedEvent()

        val failure = assertFailsWith<NotificationDeliveryException> { useCase.execute(event) }

        assertEquals(listOf(NotificationChannel.EMAIL), failure.failedChannels)
        assertEquals(1, notifications.all.size)
        assertEquals(1.0, counted("email", "failed"))
    }

    @Test
    fun `given a failed e-mail when the message is redelivered and the server is back then only the e-mail is sent`() {
        emails.failWith = EmailDeliveryException("smtp down", RuntimeException())
        val event = confirmedEvent()
        assertFailsWith<NotificationDeliveryException> { useCase.execute(event) }
        emails.failWith = null

        useCase.execute(event)

        assertEquals(1, notifications.all.size)
        assertEquals(1, emails.sent.size)
        assertEquals(1.0, counted("in_app", "duplicate"))
    }
}
