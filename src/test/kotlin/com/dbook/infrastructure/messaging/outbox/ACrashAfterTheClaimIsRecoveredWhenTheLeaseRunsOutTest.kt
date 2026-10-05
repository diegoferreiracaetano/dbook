package com.dbook.infrastructure.messaging.outbox

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class ACrashAfterTheClaimIsRecoveredWhenTheLeaseRunsOutTest : OutboxFixture() {
    @Test
    fun `given a relay that claimed an event and died when the lease runs out then another relay delivers it`() {
        val type = addEvent(dueIn = Duration.ofSeconds(-5))
        store.claim(
            clock.instant(),
            limit = 10,
            leaseUntil = clock.instant().plusSeconds(60),
        ) // claimed, never published

        assertEquals(0, relay.relayDueEvents(), "still leased")
        clock.advance(Duration.ofSeconds(61))
        assertEquals(1, relay.relayDueEvents())

        assertEquals(listOf(type), publisher.published.map { it.type })
    }
}
