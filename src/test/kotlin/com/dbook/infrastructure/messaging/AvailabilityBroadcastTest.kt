package com.dbook.infrastructure.messaging

import com.dbook.AbstractIntegrationTest
import com.dbook.application.LoginCommand
import com.dbook.application.LoginUseCase
import com.dbook.application.RegisterBookingCommand
import com.dbook.application.RegisterBookingUseCase
import com.dbook.application.RegisterFlightCommand
import com.dbook.application.RegisterFlightUseCase
import com.dbook.application.RegisterUserCommand
import com.dbook.application.RegisterUserUseCase
import com.dbook.domain.SeatClass
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

// The "test client" for item 5.4: a real STOMP-over-WebSocket connection (not a mock),
// proving a live client actually receives the broadcast — same mechanism a real KMP/
// Compose frontend would use later.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AvailabilityBroadcastTest : AbstractIntegrationTest() {
    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var registerUserUseCase: RegisterUserUseCase

    @Autowired
    lateinit var loginUseCase: LoginUseCase

    @Autowired
    lateinit var registerFlightUseCase: RegisterFlightUseCase

    @Autowired
    lateinit var registerBookingUseCase: RegisterBookingUseCase

    @Test
    fun `pushes an availability update over websocket when a booking is created`() {
        val email = "ws${(1..999_999_999).random()}@example.com"
        registerUserUseCase.execute(RegisterUserCommand(email, "s3cret-password"))
        val accessToken = loginUseCase.execute(LoginCommand(email, "s3cret-password")).accessToken

        val flight =
            registerFlightUseCase.execute(
                RegisterFlightCommand(
                    flightNumber = "DBW${(10000..99999).random()}",
                    originIataCode = "GRU",
                    destinationIataCode = "GIG",
                    departureTime = LocalDateTime.of(2027, 3, 1, 8, 0),
                    arrivalTime = LocalDateTime.of(2027, 3, 1, 9, 10),
                    seatClass = SeatClass.ECONOMY,
                    price = BigDecimal("100.00"),
                    totalCapacity = 5,
                ),
            )
        val bookableId = requireNotNull(flight.id)

        val stompClient = WebSocketStompClient(StandardWebSocketClient())
        stompClient.messageConverter = MappingJackson2MessageConverter()

        val receivedUpdates = LinkedBlockingQueue<AvailabilityUpdate>()
        val connectHeaders = StompHeaders()
        connectHeaders.add("Authorization", "Bearer $accessToken")

        val session: StompSession =
            stompClient
                .connectAsync("ws://localhost:$port/ws", null, connectHeaders, object : StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS)

        session.subscribe(
            "/topic/bookables/$bookableId/availability",
            object : StompSessionHandlerAdapter() {
                override fun getPayloadType(headers: StompHeaders) = AvailabilityUpdate::class.java

                override fun handleFrame(
                    headers: StompHeaders,
                    payload: Any?,
                ) {
                    receivedUpdates.add(payload as AvailabilityUpdate)
                }
            },
        )
        Thread.sleep(1000) // let the SUBSCRIBE frame reach the broker before triggering the event

        registerBookingUseCase.execute(RegisterBookingCommand(bookableId = bookableId, customerId = 1L))

        // Generous timeout: this is the first time this specific test ever ran against
        // real infrastructure (blocked locally by the Testcontainers/Docker Desktop
        // incompatibility documented since M4) — a CI runner has less headroom than a
        // local machine for the full round-trip (commit -> Redis publish -> subscriber
        // -> STOMP broker -> WebSocket client), so 5s turned out to be too tight there.
        val update = receivedUpdates.poll(15, TimeUnit.SECONDS)
        assertNotNull(update, "expected an availability update over the websocket connection")
        assertEquals(bookableId, update.bookableId)
        assertEquals(4, update.availableCapacity)

        session.disconnect()
    }

    @Test
    fun `rejects STOMP CONNECT without a valid token`() {
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
