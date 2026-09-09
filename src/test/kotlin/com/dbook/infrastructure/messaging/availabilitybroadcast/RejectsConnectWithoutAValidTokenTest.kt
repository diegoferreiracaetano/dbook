package com.dbook.infrastructure.messaging.availabilitybroadcast

import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertNotNull

class RejectsConnectWithoutAValidTokenTest : AvailabilityBroadcastFixture() {
    @Test
    fun `given no Authorization header when connecting then the STOMP CONNECT is rejected`() {
        val stompClient = WebSocketStompClient(StandardWebSocketClient())
        stompClient.messageConverter = MappingJackson2MessageConverter()

        val transportErrors = LinkedBlockingQueue<Throwable>()
        val handler =
            object : StompSessionHandlerAdapter() {
                override fun handleTransportError(
                    session: StompSession,
                    exception: Throwable,
                ) {
                    transportErrors.add(exception)
                }
            }

        stompClient.connectAsync("ws://localhost:$port/ws", null, StompHeaders(), handler)

        val error = transportErrors.poll(5, TimeUnit.SECONDS)
        assertNotNull(error, "expected the connection to be rejected for a missing/invalid token")
    }
}
