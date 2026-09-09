package com.dbook.infrastructure.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

// One of these runs on every app instance, each with its own Redis connection but all
// subscribed to the same channel — Redis delivers a copy of every published message to
// each of them. On receipt, this forwards to whichever local STOMP sessions (on THIS
// instance only) are subscribed to that specific bookable's topic.
@Component
class RedisAvailabilitySubscriber(
    private val messagingTemplate: SimpMessagingTemplate,
    private val objectMapper: ObjectMapper,
) : MessageListener {
    override fun onMessage(
        message: Message,
        pattern: ByteArray?,
    ) {
        val update = objectMapper.readValue(message.body, AvailabilityUpdate::class.java)
        messagingTemplate.convertAndSend("/topic/bookables/${update.bookableId}/availability", update)
    }
}
