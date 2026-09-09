package com.dbook.infrastructure.messaging.availabilitybroadcast

import com.dbook.AbstractIntegrationTest
import com.dbook.application.LoginUseCase
import com.dbook.application.RegisterBookingUseCase
import com.dbook.application.RegisterFlightUseCase
import com.dbook.application.RegisterUserUseCase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

// The "test client" for item 5.4: a real STOMP-over-WebSocket connection (not a mock),
// proving a live client actually receives the broadcast — same mechanism a real KMP/
// Compose frontend would use later.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AvailabilityBroadcastFixture : AbstractIntegrationTest() {
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
}
