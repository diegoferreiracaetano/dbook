package com.dbook.infrastructure.messaging.outbox

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class ACommitWithoutAnyRelayRunningIsDeliveredWhenTheRelayComesBackTest : OutboxFixture() {
    @Test
    fun `given an event committed while no relay ran when the relay starts then it is delivered, nothing was lost`() {
        val type = addEvent(dueIn = Duration.ofSeconds(-5))
        // the process "died" after the commit: nothing relayed, nothing marked

        relay.relayDueEvents()

        assertEquals(listOf(type), publisher.published.map { it.type })
    }
}
