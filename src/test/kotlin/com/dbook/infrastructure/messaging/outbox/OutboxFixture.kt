package com.dbook.infrastructure.messaging.outbox

import com.dbook.AbstractIntegrationTest
import com.dbook.MutableClock
import com.dbook.domain.messaging.OutboxEvent
import com.dbook.domain.messaging.OutboxWriter
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

// Delivers into a list, or fails on demand.
class RecordingPublisher : OutboxPublisher {
    val published = CopyOnWriteArrayList<ClaimedOutboxEvent>()

    @Volatile
    var failWith: String? = null

    override fun publish(event: ClaimedOutboxEvent) {
        failWith?.let { throw OutboxPublishException(it) }
        published += event
    }
}

// The real table and the real store, a hand-moved clock and a publisher that records. The table is emptied before
// each test: other tests write events too (every booking does), and a due one would be claimed here.
abstract class OutboxFixture : AbstractIntegrationTest() {
    @Autowired
    lateinit var store: OutboxStore

    @Autowired
    lateinit var writer: OutboxWriter

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var transactions: PlatformTransactionManager

    protected val clock = MutableClock()
    protected val publisher = RecordingPublisher()
    protected val meters = SimpleMeterRegistry()
    protected val properties = OutboxProperties(relay = OutboxProperties.Relay(batchSize = 10, leaseSeconds = 60))
    protected val relay by lazy { OutboxRelay(store, publisher, properties, meters, clock) }

    @BeforeEach
    fun emptyTheOutbox() {
        jdbcTemplate.update("DELETE FROM outbox_event")
    }

    protected fun inTransaction(block: () -> Unit) {
        TransactionTemplate(transactions).execute { block() }
    }

    /** Adds an event of a type of its own, due [dueIn] from the test clock's now (negative: already due). */
    protected fun addEvent(
        type: String = "test.event.${UUID.randomUUID()}",
        dueIn: Duration = Duration.ZERO,
        id: Long = 1,
    ): String {
        inTransaction {
            writer.add(OutboxEvent("test", id.toString(), type, mapOf("n" to id), clock.instant().plus(dueIn)))
        }
        return type
    }

    protected fun count(where: String = "TRUE"): Int =
        jdbcTemplate.queryForObject("SELECT count(*) FROM outbox_event WHERE $where", Int::class.java) ?: 0
}
