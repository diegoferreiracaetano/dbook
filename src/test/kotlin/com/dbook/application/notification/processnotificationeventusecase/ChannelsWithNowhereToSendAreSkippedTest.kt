package com.dbook.application.notification.processnotificationeventusecase

import com.dbook.domain.identity.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChannelsWithNowhereToSendAreSkippedTest : ProcessNotificationEventFixture() {
    @Test
    fun `given no registered device when an event is processed then the push is skipped and the rest goes out`() {
        useCase.execute(confirmedEvent())

        assertTrue(pushes.sent.isEmpty())
        assertEquals(1.0, counted("push", "skipped"))
        assertEquals(1, emails.sent.size)
    }

    @Test
    fun `given an anonymized account when an event is processed then no e-mail is sent to it`() {
        users.save(User(customerId, "anon-1@anonymized.invalid", "x", "Anonimizado", anonymizedAt = now))

        useCase.execute(confirmedEvent())

        assertTrue(emails.sent.isEmpty())
        assertEquals(1.0, counted("email", "skipped"))
    }
}
