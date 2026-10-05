package com.dbook.presentation.notification.notificationconsumer

import com.dbook.LocalStackSqs
import com.dbook.domain.catalog.FlightEvents
import com.dbook.domain.pricing.PriceAlertEvents
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class APriceEventReachesTheAlertAndTheAlertReachesTheCustomerTest : NotificationConsumerFixture() {
    @Test
    fun `given an alert when a price event and its trigger arrive then the customer finds the notification`() {
        val (token, customer) = aCustomer()
        val date = LocalDate.now().plusDays((60L..9_000L).random())
        jdbcTemplate.update(
            "INSERT INTO price_alert (user_id, origin, destination, travel_date, target_price, created_at) " +
                "VALUES (?, 'GRU', 'GIG', ?, 300, now())",
            customer,
            java.sql.Date.valueOf(date),
        )
        val alertId =
            jdbcTemplate.queryForObject(
                "SELECT id FROM price_alert WHERE user_id = ?",
                Long::class.java,
                customer,
            )
        val queue = LocalStackSqs.createQueue()
        sendRaw(
            queue,
            FlightEvents.PRICE_CHANGED,
            UUID.randomUUID(),
            """{"flightId":1,"flightNumber":"DB1","origin":"GRU","destination":"GIG",""" +
                """"date":"$date","price":"250.00"}""",
        )

        consumerFor(queue).poll()

        val trigger =
            jdbcTemplate.queryForObject(
                "SELECT id::text FROM outbox_event WHERE type = ? AND aggregate_id = ?",
                String::class.java,
                PriceAlertEvents.TRIGGERED,
                alertId.toString(),
            )
        val payload =
            jdbcTemplate.queryForObject(
                "SELECT payload::text FROM outbox_event WHERE id = ?::uuid",
                String::class.java,
                trigger,
            )
        sendRaw(queue, PriceAlertEvents.TRIGGERED, UUID.fromString(trigger), payload)
        consumerFor(queue).poll()

        val items = body(inbox(token))["items"]
        assertEquals(listOf("PRICE_ALERT"), items.map { it["type"].asText() })
        assertEquals("Alerta de preço", items[0]["title"].asText())
    }
}
