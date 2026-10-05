package com.dbook.infrastructure.messaging.availability.availabilitybroadcast

import com.dbook.application.identity.LoginCommand
import com.dbook.application.identity.RegisterUserCommand
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
import kotlin.test.assertTrue

class TheVersionedWebsocketEndpointAnswersLikeTheOldOneTest : AvailabilityBroadcastFixture() {
    private fun client() =
        WebSocketStompClient(StandardWebSocketClient()).also { it.messageConverter = MappingJackson2MessageConverter() }

    private fun tokenOfANewUser(): String {
        val email = "ws${(1..999_999_999).random()}@example.com"
        registerUserUseCase.execute(RegisterUserCommand(email, "s3cret-password", "WS Test User"))
        return loginUseCase.execute(LoginCommand(email, "s3cret-password", clientIp = "127.0.0.1")).accessToken
    }

    @Test
    fun `given a valid token when connecting to the versioned and the old endpoints then both accept`() {
        val token = tokenOfANewUser()

        listOf("/v1/ws", "/ws").forEach { path ->
            val headers = StompHeaders().also { it.add("Authorization", "Bearer $token") }
            val session: StompSession =
                client().connectAsync(
                    "ws://localhost:$port$path",
                    null,
                    headers,
                    object : StompSessionHandlerAdapter() {},
                )
                    .get(5, TimeUnit.SECONDS)
            assertTrue(session.isConnected, "$path should accept a valid token")
            session.disconnect()
        }
    }

    @Test
    fun `given no token when connecting to the versioned endpoint then the CONNECT is rejected`() {
        val errors = LinkedBlockingQueue<Throwable>()
        val handler =
            object : StompSessionHandlerAdapter() {
                override fun handleTransportError(
                    session: StompSession,
                    exception: Throwable,
                ) {
                    errors.add(exception)
                }
            }

        client().connectAsync("ws://localhost:$port/v1/ws", null, StompHeaders(), handler)

        assertNotNull(errors.poll(5, TimeUnit.SECONDS), "expected the connection to be rejected without a token")
    }
}
