package com.dbook.infrastructure.messaging.outbox

import com.dbook.domain.messaging.OutboxEvent
import org.springframework.transaction.IllegalTransactionStateException
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AnEventOutsideATransactionIsRefusedTest : OutboxFixture() {
    @Test
    fun `given no transaction when adding an event then it is refused, as it could outlive its change`() {
        assertFailsWith<IllegalTransactionStateException> {
            writer.add(OutboxEvent("test", "1", "test.alone.event", emptyMap(), Instant.now()))
        }

        assertEquals(0, count("type = 'test.alone.event'"))
    }
}
