package com.dbook.infrastructure.messaging.outbox

import java.sql.Timestamp
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class CleaningUpDeletesOnlyWhatWasPublishedLongAgoTest : OutboxFixture() {
    @Test
    fun `given old and recent published and unpublished events when cleaning up then only the old published go`() {
        addEvent("test.old.event", dueIn = Duration.ofSeconds(-1), id = 1)
        addEvent("test.recent.event", dueIn = Duration.ofSeconds(-1), id = 2)
        addEvent("test.waiting.event", dueIn = Duration.ofDays(1), id = 3)
        relay.relayDueEvents()
        jdbcTemplate.update(
            "UPDATE outbox_event SET published_at = ? WHERE type = 'test.old.event'",
            Timestamp.from(clock.instant().minus(Duration.ofDays(8))),
        )

        val deleted = relay.cleanUp()

        assertEquals(1, deleted)
        assertEquals(0, count("type = 'test.old.event'"))
        assertEquals(1, count("type = 'test.recent.event'"))
        assertEquals(1, count("type = 'test.waiting.event'"))
    }
}
