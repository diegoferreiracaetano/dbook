package com.dbook.infrastructure.messaging

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val stompAuthChannelInterceptor: StompAuthChannelInterceptor,
) : WebSocketMessageBrokerConfigurer {
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        // no withSockJS(): the frontend is a separate KMP/Compose client (project
        // decision), not a browser page needing SockJS's HTTP-polling fallback.
        registry.addEndpoint("/ws")
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        // In-memory broker: still only delivers to THIS instance's own connected
        // sessions. Cross-instance fan-out is handled separately by
        // RedisAvailabilitySubscriber, which forwards messages received from Redis
        // into this same broker.
        registry.enableSimpleBroker("/topic")
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(stompAuthChannelInterceptor)
    }
}
