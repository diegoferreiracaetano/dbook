package com.dbook.application.notification.processnotificationeventusecase

import kotlin.test.Test
import kotlin.test.assertEquals

class AnEventIsDeliveredOnEveryChannelTest : ProcessNotificationEventFixture() {
    @Test
    fun `given a customer with a device when a booking is confirmed then inbox, e-mail and push all go out`() {
        registerDevice()

        useCase.execute(confirmedEvent())

        assertEquals(listOf("Reserva confirmada"), notifications.all.map { it.title })
        assertEquals(listOf("ana@example.com"), emails.sent.map { it.to })
        assertEquals(listOf("1:Reserva confirmada"), pushes.sent)
        assertEquals(1.0, counted("in_app", "sent"))
        assertEquals(1.0, counted("email", "sent"))
        assertEquals(1.0, counted("push", "sent"))
    }
}
