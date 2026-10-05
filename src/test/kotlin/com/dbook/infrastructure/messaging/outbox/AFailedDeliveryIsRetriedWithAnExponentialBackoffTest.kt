package com.dbook.infrastructure.messaging.outbox

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class AFailedDeliveryIsRetriedWithAnExponentialBackoffTest : OutboxFixture() {
    @Test
    fun `given a publisher that fails when the relay runs then it backs off 5 s, 10 s and delivers when it recovers`() {
        val type = addEvent(dueIn = Duration.ofSeconds(-1))
        publisher.failWith = "queue unreachable"

        assertEquals(0, relay.relayDueEvents())
        assertEquals(1, count("type = '$type' AND attempts = 1 AND last_error = 'queue unreachable'"))
        clock.advance(Duration.ofSeconds(4))
        assertEquals(0, relay.relayDueEvents())
        assertEquals(1, count("type = '$type' AND attempts = 1"), "not yet: the backoff is 5 s")

        clock.advance(Duration.ofSeconds(2))
        relay.relayDueEvents()
        assertEquals(1, count("type = '$type' AND attempts = 2"))
        clock.advance(Duration.ofSeconds(9))
        assertEquals(0, relay.relayDueEvents())

        publisher.failWith = null
        clock.advance(Duration.ofSeconds(2))
        assertEquals(1, relay.relayDueEvents())
        assertEquals(listOf(type), publisher.published.map { it.type })
        assertEquals(1, count("type = '$type' AND published_at IS NOT NULL AND last_error IS NULL AND attempts = 3"))
        assertEquals(2.0, meters.counter("dbook.outbox.failures", "type", type).count())
    }
}
