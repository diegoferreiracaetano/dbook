package com.dbook.presentation.payment.paymentcontroller

import com.dbook.application.payment.RegisterPaymentCommand
import com.dbook.application.payment.RegisterPaymentUseCase
import com.dbook.domain.identity.TokenService
import com.dbook.domain.payment.Payment
import com.dbook.presentation.payment.PaymentController
import com.dbook.presentation.payment.RegisterPaymentRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import java.math.BigDecimal

// HTTP behavior only — security filters are off (addFilters = false), so the authenticated
// user is handed to the controller directly through `principal` instead of a JWT.
@WebMvcTest(PaymentController::class)
@AutoConfigureMockMvc(addFilters = false)
abstract class PaymentControllerFixture {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var registerPaymentUseCase: RegisterPaymentUseCase

    // see FlightSearchControllerFixture for why this is still needed despite addFilters = false
    @MockBean
    lateinit var tokenService: TokenService

    protected val objectMapper = ObjectMapper()
    protected val userId = 1L
    protected val idempotencyKey = "3f2b8c1e-6a4d-4e7a-9d1b-5c8e2a7f0b94"
    protected val authentication = UsernamePasswordAuthenticationToken(userId.toString(), null, emptyList())

    protected val request = RegisterPaymentRequest(listOf(100L, 101L), "4242", "Jane Doe")

    protected val expectedCommand =
        RegisterPaymentCommand(
            bookingIds = request.bookingIds,
            cardLast4 = request.cardLast4,
            cardholderName = request.cardholderName,
            requestingUserId = userId,
            idempotencyKey = idempotencyKey,
        )

    protected val payment =
        Payment(
            id = 7,
            customerId = userId,
            amount = BigDecimal("800.00"),
            cardLast4 = "4242",
            cardholderName = "Jane Doe",
            idempotencyKey = idempotencyKey,
        )
}
