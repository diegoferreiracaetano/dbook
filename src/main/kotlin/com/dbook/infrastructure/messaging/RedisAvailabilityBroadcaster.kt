package com.dbook.infrastructure.messaging

import com.dbook.domain.AvailabilityBroadcaster
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

// Publishes to Redis instead of pushing to this JVM's own WebSocket sessions directly
// (what SimpMessagingAvailabilityBroadcaster did in 5.1-5.4). Every app instance
// subscribes to the same channel (RedisAvailabilitySubscriber) and forwards to its own
// local STOMP sessions — this is what makes the broadcast work across instances, not
// just within the one that handled the request.
@Component
class RedisAvailabilityBroadcaster(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : AvailabilityBroadcaster {
    override fun broadcast(
        bookableId: Long,
        availableCapacity: Int,
    ) {
        val payload = objectMapper.writeValueAsString(AvailabilityUpdate(bookableId, availableCapacity))
        redisTemplate.convertAndSend(AVAILABILITY_CHANNEL, payload)
    }

    companion object {
        const val AVAILABILITY_CHANNEL = "dbook:availability"
    }
}
