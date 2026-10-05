package com.dbook.application.common

import com.dbook.domain.messaging.OutboxEvent
import com.dbook.domain.messaging.OutboxWriter

class RecordingOutboxWriter : OutboxWriter {
    val events = mutableListOf<OutboxEvent>()

    override fun add(event: OutboxEvent) {
        events += event
    }

    fun ofType(type: String): List<OutboxEvent> = events.filter { it.type == type }
}
