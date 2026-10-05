package com.dbook.infrastructure.messaging.outbox

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/** Runs the relay on a timer. Off in the tests (`outbox.relay.enabled=false`), which call the relay themselves. */
@Component
@ConditionalOnProperty(name = ["outbox.relay.enabled"], havingValue = "true", matchIfMissing = true)
class ScheduledOutboxRelay(
    private val outboxRelay: OutboxRelay,
) {
    @Scheduled(fixedDelayString = "\${outbox.relay.delay-ms:1000}")
    fun relay() {
        outboxRelay.relayDueEvents()
    }

    @Scheduled(cron = "\${outbox.relay.cleanup-cron:0 30 3 * * *}")
    fun cleanUp() {
        outboxRelay.cleanUp()
    }
}
