package com.dbook.presentation.securityintegration

import com.dbook.AbstractIntegrationTest
import com.dbook.application.RegisterFlightCommand
import com.dbook.application.RegisterFlightUseCase
import com.dbook.domain.Role
import com.dbook.domain.SeatClass
import com.dbook.domain.User
import com.dbook.domain.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.LocalDateTime

// Exercises the real SecurityFilterChain + @PreAuthorize + JWT rotation end to end
// (item 3.8), against a Postgres provisioned by Testcontainers (item 4.2, no manual
// setup needed). Deliberately a full @SpringBootTest rather than @WebMvcTest slices:
// the custom SecurityConfig, JwtAuthenticationFilter and @EnableMethodSecurity are
// only reliably exercised together against the real application context.
@AutoConfigureMockMvc
abstract class SecurityIntegrationFixture : AbstractIntegrationTest() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var registerFlightUseCase: RegisterFlightUseCase

    protected fun uniqueEmail() = "user${(1..999_999_999).random()}@example.com"

    protected fun registerAndLogin(
        email: String,
        password: String = "s3cret-password",
    ): String {
        mockMvc.post("/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("email" to email, "password" to password))
        }
        return loginAccessToken(email, password)
    }

    protected fun loginAccessToken(
        email: String,
        password: String,
    ): String {
        val result =
            mockMvc.post("/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("email" to email, "password" to password))
            }.andReturn()
        return objectMapper.readTree(result.response.contentAsString)["accessToken"].asText()
    }

    protected fun loginRefreshToken(
        email: String,
        password: String,
    ): String {
        val result =
            mockMvc.post("/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("email" to email, "password" to password))
            }.andReturn()
        return objectMapper.readTree(result.response.contentAsString)["refreshToken"].asText()
    }

    // No self-promotion endpoint exists on purpose (see README) — promoting a user for
    // test setup goes straight through the repository, bypassing the public API.
    protected fun registerAdminAndLogin(
        email: String,
        password: String = "s3cret-password",
    ): String {
        registerAndLogin(email, password)
        val user = requireNotNull(userRepository.findByEmail(email))
        userRepository.save(User(id = user.id, email = user.email, passwordHash = user.passwordHash, role = Role.ADMIN))
        return loginAccessToken(email, password)
    }

    protected fun validFlightRequestBody(): String =
        objectMapper.writeValueAsString(
            mapOf(
                "flightNumber" to "DBA${(10000..99999).random()}",
                "originIataCode" to "GRU",
                "destinationIataCode" to "GIG",
                "departureTime" to "2027-02-01T08:00:00",
                "arrivalTime" to "2027-02-01T09:10:00",
                "seatClass" to "ECONOMY",
                "price" to 100.00,
                "totalCapacity" to 10,
            ),
        )

    protected fun registerFlightWithOneSeat(): Long {
        val flight =
            registerFlightUseCase.execute(
                RegisterFlightCommand(
                    flightNumber = "DBS${(10000..99999).random()}",
                    originIataCode = "GRU",
                    destinationIataCode = "GIG",
                    departureTime = LocalDateTime.of(2027, 1, 1, 8, 0),
                    arrivalTime = LocalDateTime.of(2027, 1, 1, 9, 10),
                    seatClass = SeatClass.ECONOMY,
                    price = BigDecimal("100.00"),
                    totalCapacity = 1,
                ),
            )
        return requireNotNull(flight.id)
    }
}
