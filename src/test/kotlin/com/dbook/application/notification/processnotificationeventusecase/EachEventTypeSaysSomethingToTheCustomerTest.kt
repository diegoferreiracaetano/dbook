package com.dbook.application.notification.processnotificationeventusecase

import com.dbook.application.notification.NotificationEvent
import com.dbook.domain.notification.NotificationType
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EachEventTypeSaysSomethingToTheCustomerTest : ProcessNotificationEventFixture() {
    @Test
    fun `given every notification type when processed then each one has its own title and the data it carries`() {
        NotificationType.entries.forEach { type ->
            useCase.execute(
                NotificationEvent(
                    UUID.randomUUID(),
                    type,
                    mapOf(
                        "customerId" to customerId,
                        "title" to "GRU-GIG",
                        "amount" to "100.50",
                        "changes" to listOf("departureTime"),
                    ),
                ),
            )
        }

        assertEquals(NotificationType.entries.size, notifications.all.map { it.title }.toSet().size)
        assertTrue(notifications.all.single { it.type == NotificationType.REFUND_COMPLETED }.body.contains("R$ 100,50"))
        assertTrue(
            notifications.all.single { it.type == NotificationType.FLIGHT_CHANGED }.body.contains("horário de partida"),
        )
    }

    @Test
    fun `given an event type name when it is looked up then the type is found, and an unknown one is not`() {
        assertEquals(NotificationType.REFUND_COMPLETED, NotificationType.fromEventType("refund.completed"))
        assertEquals(null, NotificationType.fromEventType("booking.teleported"))
    }
}
