package com.dbook.infrastructure.messaging

import com.dbook.domain.AvailabilityBroadcaster
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

// Direct, single-instance broadcast: pushes straight to this JVM's own connected STOMP
// sessions. Works fine with one instance, but a second app instance would never see
// this — that's exactly the gap Redis Pub/Sub (item 5.6) closes.
@Component
class SimpMessagingAvailabilityBroadcaster(
    private val messagingTemplate: SimpMessagingTemplate,
) : AvailabilityBroadcaster {
    override fun broadcast(bookableId: Long, availableCapacity: Int) {
        messagingTemplate.convertAndSend(
            "/topic/bookables/$bookableId/availability",
            AvailabilityUpdate(bookableId, availableCapacity),
        )
    }
}
