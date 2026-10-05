package com.dbook.application.notification.processnotificationeventusecase

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class ADuplicateDeliveryOfTheSameEventSendsNothingAgainTest : ProcessNotificationEventFixture() {
    @Test
    fun `given an event already processed when the queue delivers it again then nothing is sent a second time`() {
        registerDevice()
        val event = confirmedEvent(UUID.randomUUID())
        useCase.execute(event)

        useCase.execute(event)

        assertEquals(1, notifications.all.size)
        assertEquals(1, emails.sent.size)
        assertEquals(1, pushes.sent.size)
        assertEquals(1.0, counted("email", "duplicate"))
    }
}
