package com.dbook.presentation

import com.dbook.application.RegisterFlightCommand
import com.dbook.application.RegisterFlightUseCase
import com.dbook.domain.Role
import com.dbook.domain.SeatClass
import com.dbook.domain.User
import com.dbook.domain.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertNotEquals

// Exercises the real SecurityFilterChain + @PreAuthorize + JWT rotation end to end
// (item 3.8) — needs `docker compose up -d` running, same as BookingConcurrencyTest.
// This is deliberately a full @SpringBootTest rather than @WebMvcTest slices: the
// custom SecurityConfig, JwtAuthenticationFilter and @EnableMethodSecurity are only
// reliably exercised together against the real application context.
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var registerFlightUseCase: RegisterFlightUseCase

    private fun uniqueEmail() = "user${(1..999_999_999).random()}@example.com"

    private fun registerAndLogin(
        email: String,
        password: String = "s3cret-password",
    ): String {
        mockMvc.post("/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("email" to email, "password" to password))
        }
        return loginAccessToken(email, password)
    }

    private fun loginAccessToken(
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

    private fun loginRefreshToken(
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
    private fun registerAdminAndLogin(
        email: String,
        password: String = "s3cret-password",
    ): String {
        val token = registerAndLogin(email, password)
        val user = requireNotNull(userRepository.findByEmail(email))
        userRepository.save(User(id = user.id, email = user.email, passwordHash = user.passwordHash, role = Role.ADMIN))
        return loginAccessToken(email, password)
    }

    private fun validFlightRequestBody(): String =
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

    private fun registerFlightWithOneSeat(): Long {
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

    @Test
    fun `admin endpoint rejects requests without a token`() {
        mockMvc.post("/admin/flights") {
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `admin endpoint rejects a CLIENT token`() {
        val token = registerAndLogin(uniqueEmail())

        // A well-formed body on purpose: @PreAuthorize is method-level AOP, which only
        // runs once Spring MVC has already bound the request body. A malformed body
        // (e.g. "{}") would 400 before authorization is even checked, hiding the thing
        // this test is actually about.
        mockMvc.post("/admin/flights") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = validFlightRequestBody()
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `admin endpoint accepts an ADMIN token`() {
        val token = registerAdminAndLogin(uniqueEmail())

        mockMvc.post("/admin/flights") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = validFlightRequestBody()
        }.andExpect { status { isCreated() } }
    }

    @Test
    fun `booking endpoints require authentication`() {
        mockMvc.post("/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `a client can book with a valid token`() {
        val token = registerAndLogin(uniqueEmail())
        val bookableId = registerFlightWithOneSeat()

        mockMvc.post("/bookings") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("bookableId" to bookableId))
        }.andExpect { status { isCreated() } }
    }

    @Test
    fun `a client cannot cancel someone else's booking, but an admin can`() {
        val ownerEmail = uniqueEmail()
        val ownerToken = registerAndLogin(ownerEmail)
        val otherToken = registerAndLogin(uniqueEmail())
        val adminToken = registerAdminAndLogin(uniqueEmail())
        val bookableId = registerFlightWithOneSeat()

        val bookingResult =
            mockMvc.post("/bookings") {
                header("Authorization", "Bearer $ownerToken")
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("bookableId" to bookableId))
            }.andReturn()
        val bookingId = objectMapper.readTree(bookingResult.response.contentAsString)["id"].asLong()

        mockMvc.post("/bookings/$bookingId/cancel") {
            header("Authorization", "Bearer $otherToken")
        }.andExpect { status { isForbidden() } }

        mockMvc.post("/bookings/$bookingId/cancel") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect { status { isOk() } }
    }

    @Test
    fun `a used refresh token cannot be reused (rotation)`() {
        val email = uniqueEmail()
        registerAndLogin(email)
        val refreshToken = loginRefreshToken(email, "s3cret-password")

        mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("refreshToken" to refreshToken))
        }.andExpect { status { isOk() } }

        mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("refreshToken" to refreshToken))
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `refreshing issues a new, different access token`() {
        val email = uniqueEmail()
        registerAndLogin(email)
        val refreshToken = loginRefreshToken(email, "s3cret-password")
        val originalAccessToken = loginAccessToken(email, "s3cret-password")

        val refreshResult =
            mockMvc.post("/auth/refresh") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("refreshToken" to refreshToken))
            }.andReturn()
        val newAccessToken = objectMapper.readTree(refreshResult.response.contentAsString)["accessToken"].asText()

        assertNotEquals(originalAccessToken, newAccessToken)
    }

    @Test
    fun `search remains public without a token`() {
        mockMvc.get("/flights/search") {
            param("origin", "GRU")
            param("destination", "GIG")
            param("date", "2027-01-01")
        }.andExpect { status { isOk() } }
    }
}
