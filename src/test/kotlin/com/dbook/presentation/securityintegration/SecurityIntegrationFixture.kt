package com.dbook.presentation.securityintegration

import com.dbook.AbstractIntegrationTest
import com.dbook.application.catalog.RegisterFlightCommand
import com.dbook.application.catalog.RegisterFlightUseCase
import com.dbook.application.identity.BlockUserCommand
import com.dbook.application.identity.BlockUserUseCase
import com.dbook.domain.catalog.SeatClass
import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.Role
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserRepository
import com.dbook.domain.seating.SeatRepository
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.Cookie
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
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

    @Autowired
    lateinit var seatRepository: SeatRepository

    @Autowired
    lateinit var blockUserUseCase: BlockUserUseCase

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    protected fun uniqueEmail() = "user${(1..999_999_999).random()}@example.com"

    protected fun registerAndLogin(
        email: String,
        password: String = "s3cret-password",
        name: String = "Test User",
    ): String {
        mockMvc.post("/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(
                    mapOf("email" to email, "password" to password, "name" to name),
                )
        }
        return loginAccessToken(email, password)
    }

    protected fun loginAccessToken(
        email: String,
        password: String,
    ): String {
        val result =
            mockMvc.post("/v1/auth/login") {
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
            mockMvc.post("/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("email" to email, "password" to password))
            }.andReturn()
        return objectMapper.readTree(result.response.contentAsString)["refreshToken"].asText()
    }

    // no self-promotion endpoint exists on purpose: test setup promotes through the repository
    protected fun registerStaffAndLogin(
        email: String,
        role: Role = Role.SUPER_ADMIN,
        password: String = "s3cret-password",
    ): String {
        registerStaff(email, role, password)
        return loginAccessToken(email, password)
    }

    protected fun registerStaff(
        email: String,
        role: Role = Role.SUPER_ADMIN,
        password: String = "s3cret-password",
    ) {
        registerAndLogin(email, password)
        changeRole(email, role)
    }

    protected fun changeRole(
        email: String,
        role: Role,
    ) {
        val user = requireNotNull(userRepository.findByEmail(email))
        userRepository.save(
            User(
                id = user.id,
                email = user.email,
                passwordHash = user.passwordHash,
                name = user.name,
                role = role,
                version = user.version,
            ),
        )
    }

    protected fun block(
        email: String,
        reason: String = "chargeback fraud",
    ) {
        blockUserUseCase.execute(BlockUserCommand(requireNotNull(userRepository.findByEmail(email)?.id), reason))
    }

    protected fun adminLogin(
        email: String,
        password: String = "s3cret-password",
    ): MvcResult =
        mockMvc.post("/v1/admin/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("email" to email, "password" to password))
        }.andReturn()

    /** The value of the refresh cookie a response sets (what the browser would send back). */
    protected fun refreshCookieOf(result: MvcResult): String =
        requireNotNull(result.response.getHeader("Set-Cookie")).substringAfter("=").substringBefore(";")

    protected fun adminRefresh(
        cookieValue: String?,
        origin: String? = PORTAL_ORIGIN,
    ): ResultActionsDsl =
        mockMvc.post("/v1/admin/auth/refresh") {
            if (origin != null) header("Origin", origin)
            if (cookieValue != null) cookie(Cookie("dbook_admin_refresh", cookieValue))
        }

    protected fun errorCodeOf(result: MvcResult): String =
        objectMapper.readTree(result.response.contentAsString)["code"].asText()

    protected fun reprice(
        bookableId: Long,
        price: String,
    ) {
        jdbcTemplate.update("UPDATE bookable SET price = ? WHERE id = ?", BigDecimal(price), bookableId)
    }

    /** @return the id of the new PENDING booking. */
    protected fun book(
        token: String,
        bookableId: Long,
        seatId: Long,
    ): Long {
        val result =
            mockMvc.post("/v1/bookings") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("bookableId" to bookableId, "seatId" to seatId))
            }.andReturn()
        return objectMapper.readTree(result.response.contentAsString)["id"].asLong()
    }

    protected fun userIdOf(email: String): Long = requireNotNull(userRepository.findByEmail(email)?.id)

    protected fun auditEntries(
        token: String,
        query: String,
    ): JsonNode =
        objectMapper.readTree(
            mockMvc.get("/v1/admin/audit?$query") { header("Authorization", "Bearer $token") }
                .andReturn().response.contentAsString,
        )["items"]

    protected fun validFlightRequestBody(): String =
        objectMapper.writeValueAsString(
            mapOf(
                "flightNumber" to "DBA${(10000..99999).random()}",
                "airlineIataCode" to "LA",
                "originIataCode" to "GRU",
                "destinationIataCode" to "GIG",
                "departureTime" to "2027-02-01T08:00:00",
                "arrivalTime" to "2027-02-01T09:10:00",
                "seatClass" to "ECONOMY",
                "price" to 100.00,
                "totalCapacity" to 10,
                "aircraftType" to "Airbus A320",
            ),
        )

    /** @return the new flight's bookableId and its one generated seatId. */
    protected fun registerFlightWithOneSeat(destinationIataCode: String = "GIG"): Pair<Long, Long> {
        val flight =
            registerFlightUseCase.execute(
                RegisterFlightCommand(
                    actor = Actor(id = 1, role = Role.SUPER_ADMIN),
                    flightNumber = "DBS${(10000..99999).random()}",
                    airlineIataCode = "LA",
                    originIataCode = "GRU",
                    destinationIataCode = destinationIataCode,
                    departureTime = LocalDateTime.of(2027, 1, 1, 8, 0),
                    arrivalTime = LocalDateTime.of(2027, 1, 1, 9, 10),
                    seatClass = SeatClass.ECONOMY,
                    price = BigDecimal("100.00"),
                    totalCapacity = 1,
                    aircraftType = "Airbus A320",
                ),
            )
        val bookableId = requireNotNull(flight.id)
        val seatId = requireNotNull(seatRepository.findByBookableId(bookableId).first().id)
        return bookableId to seatId
    }

    protected companion object {
        const val PORTAL_ORIGIN = "http://localhost:3000"
    }
}
