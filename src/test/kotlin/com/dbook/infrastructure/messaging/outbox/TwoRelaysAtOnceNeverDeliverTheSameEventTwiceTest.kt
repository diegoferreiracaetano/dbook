package com.dbook.infrastructure.messaging.outbox

import com.dbook.MutableClock
import java.time.Duration
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals

class TwoRelaysAtOnceNeverDeliverTheSameEventTwiceTest : OutboxFixture() {
    @Test
    fun `given 60 due events and two relays running at once when they run then each event is delivered exactly once`() {
        repeat(60) { addEvent("test.race.event", dueIn = Duration.ofSeconds(-5), id = it.toLong()) }
        val barrier = CyclicBarrier(2)
        val pool = Executors.newFixedThreadPool(2)

        try {
            List(2) {
                pool.submit {
                    barrier.await()
                    OutboxRelay(store, publisher, properties, meters, MutableClock(clock.instant())).relayDueEvents()
                }
            }.forEach { it.get() }
        } finally {
            pool.shutdown()
        }

        val ids = publisher.published.map { it.id }
        assertEquals(60, ids.size)
        assertEquals(60, ids.toSet().size, "an event was delivered twice")
        assertEquals(60, count("type = 'test.race.event' AND published_at IS NOT NULL"))
    }
}
