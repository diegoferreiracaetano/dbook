package com.dbook.infrastructure.messaging.outbox

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class ADeliveredEventIsNotDeliveredAgainTest : OutboxFixture() {
    @Test
    fun `given a due event when the relay runs twice then it is delivered once and marked`() {
        val type = addEvent(dueIn = Duration.ofSeconds(-1))

        val first = relay.relayDueEvents()
        clock.advance(Duration.ofMinutes(10))
        val second = relay.relayDueEvents()

        assertEquals(1, first)
        assertEquals(0, second)
        assertEquals(listOf(type), publisher.published.map { it.type })
        assertEquals(1, count("type = '$type' AND published_at IS NOT NULL"))
        assertEquals(1.0, meters.counter("dbook.outbox.published", "type", type).count())
    }
}
