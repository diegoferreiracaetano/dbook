package com.dbook.infrastructure.messaging.outbox

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class TheStatsSayHowManyAreWaitingAndHowLateTheWorstIsTest : OutboxFixture() {
    @Test
    fun `given a future and an overdue event when reading the stats then pending counts both, overdue is the worst`() {
        addEvent("test.future.event", dueIn = Duration.ofMinutes(15), id = 1)
        addEvent("test.late.event", dueIn = Duration.ofSeconds(-120), id = 2)
        publisher.failWith = "down"
        relay.relayDueEvents() // the late one failed: still overdue, however far its retry is pushed
        publisher.failWith = null

        val stats = store.stats(clock.instant())

        assertEquals(2, stats.pending)
        assertEquals(120, stats.overdueSeconds, "late by its original due time, not by its retry time")
    }
}
