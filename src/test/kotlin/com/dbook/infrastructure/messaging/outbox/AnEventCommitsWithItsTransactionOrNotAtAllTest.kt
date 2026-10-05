package com.dbook.infrastructure.messaging.outbox

import com.dbook.domain.messaging.OutboxEvent
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class AnEventCommitsWithItsTransactionOrNotAtAllTest : OutboxFixture() {
    @Test
    fun `given an event added in a transaction when it commits then it exists, and on a rollback it does not`() {
        inTransaction { writer.add(OutboxEvent("test", "1", "test.committed.event", mapOf("n" to 1), Instant.now())) }
        runCatching {
            inTransaction {
                writer.add(OutboxEvent("test", "2", "test.rolledback.event", mapOf("n" to 2), Instant.now()))
                error("the change failed after the event was added")
            }
        }

        assertEquals(1, count("type = 'test.committed.event'"))
        assertEquals(0, count("type = 'test.rolledback.event'"))
    }
}
