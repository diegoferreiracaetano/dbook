package com.dbook.infrastructure.messaging.outbox

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnlyDueEventsAreClaimedOldestFirstAndEachOnceTest : OutboxFixture() {
    @Test
    fun `given due, leased and future events when claiming then only due ones come, once, and again after the lease`() {
        val first = addEvent("test.a.event", dueIn = Duration.ofSeconds(-30), id = 1)
        val second = addEvent("test.b.event", dueIn = Duration.ofSeconds(-10), id = 2)
        addEvent("test.c.event", dueIn = Duration.ofMinutes(15), id = 3)
        val lease = clock.instant().plusSeconds(60)

        val claimed = store.claim(clock.instant(), limit = 10, leaseUntil = lease)
        val again = store.claim(clock.instant(), limit = 10, leaseUntil = lease)

        assertEquals(listOf(first, second), claimed.map { it.type })
        assertEquals(listOf(1, 1), claimed.map { it.attempts })
        assertTrue(again.isEmpty(), "a leased event must not be claimed by another relay")

        clock.advance(Duration.ofSeconds(61))
        val afterTheLease = store.claim(clock.instant(), limit = 10, leaseUntil = clock.instant().plusSeconds(60))
        assertEquals(listOf(first, second), afterTheLease.map { it.type })
        assertEquals(listOf(2, 2), afterTheLease.map { it.attempts })
    }
}
