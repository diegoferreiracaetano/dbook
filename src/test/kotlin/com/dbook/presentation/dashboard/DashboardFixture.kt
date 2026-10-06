package com.dbook.presentation.dashboard

import com.dbook.application.flight.RegisterFlightCommand
import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.access.Role
import com.dbook.domain.flight.SeatClass
import com.dbook.presentation.securityintegration.SecurityIntegrationFixture
import com.fasterxml.jackson.databind.JsonNode
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.get
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

// The dashboard counts what happened in a period, and the database is shared by every integration test, so each test
// works in its own window of days in 2023 (nothing else ever writes there, and Sao Paulo had no daylight saving time
// then, so its offset is always -03:00) and inserts its history with the exact timestamps it wants: through the API
// the moments would all be "now".
abstract class DashboardFixture : SecurityIntegrationFixture() {
    @Autowired
    lateinit var redis: StringRedisTemplate

    @Autowired
    lateinit var meters: MeterRegistry

    @org.junit.jupiter.api.BeforeEach
    fun clearTheCache() {
        redis.keys("dbook:dashboard:*").takeIf { it.isNotEmpty() }?.let { redis.delete(it) }
    }

    /** A window of 10 days that no other test uses. */
    protected fun newWindow(): LocalDate = LocalDate.of(2023, 1, 1).plusDays(windows.getAndIncrement() * 20L)

    protected fun staff(role: Role = Role.SUPPORT): String = registerStaffAndLogin(uniqueEmail(), role)

    protected fun at(
        day: LocalDate,
        time: String = "12:00:00",
    ): Instant = Instant.parse("${day}T${time}Z")

    protected fun customerCreatedAt(at: Instant): Long =
        jdbcTemplate.queryForObject(
            "INSERT INTO app_user (email, password_hash, name, role, created_at) " +
                "VALUES (?, 'x', 'Dash Customer', 'CLIENT', ?) RETURNING id",
            Long::class.java,
            uniqueEmail(),
            Timestamp.from(at),
        ) ?: error("no id")

    /** A flight on [departure] with a real seat map; returns its id. */
    protected fun flightOn(
        departure: LocalDateTime,
        capacity: Int = 12,
        origin: String = "GRU",
        destination: String = "GIG",
    ): Long =
        registerFlightUseCase.execute(
            RegisterFlightCommand(
                Actor(1, Role.SUPER_ADMIN), "DSH${(10000..99999).random()}", "LA", origin, destination, departure,
                departure.plusHours(1), SeatClass.ECONOMY, BigDecimal("100.00"), capacity, "Airbus A320",
            ),
        ).id ?: error("no id")

    protected fun reserveSeats(
        flightId: Long,
        count: Int,
    ) {
        jdbcTemplate.update(
            "UPDATE seat SET status = 'RESERVED' WHERE id IN " +
                "(SELECT id FROM seat WHERE bookable_id = ? ORDER BY id LIMIT ?)",
            flightId,
            count,
        )
    }

    /**
     * A booking with the history the given moments imply: created, then each of [steps] (status, moment, actor).
     * The booking's own status is the last step's.
     */
    protected fun booking(
        customerId: Long,
        flightId: Long,
        price: String,
        createdAt: Instant,
        vararg steps: Triple<String, Instant, Long?>,
    ): Long {
        val seat =
            jdbcTemplate.queryForObject(
                "SELECT min(id) FROM seat WHERE bookable_id = ?",
                Long::class.java,
                flightId,
            )
        val status = steps.lastOrNull()?.first ?: "PENDING"
        val id =
            jdbcTemplate.queryForObject(
                "INSERT INTO booking (bookable_id, customer_id, status, seat_id, price, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
                Long::class.java, flightId, customerId, status, seat, BigDecimal(price), Timestamp.from(createdAt),
            ) ?: error("no id")
        history(id, null, "PENDING", null, createdAt)
        var previous = "PENDING"
        steps.forEach { (to, moment, actor) ->
            history(id, previous, to, actor, moment)
            previous = to
        }
        return id
    }

    private fun history(
        bookingId: Long,
        from: String?,
        to: String,
        actor: Long?,
        at: Instant,
    ) {
        jdbcTemplate.update(
            "INSERT INTO booking_status_history (booking_id, from_status, to_status, actor_id, occurred_at) " +
                "VALUES (?, ?, ?, ?, ?)",
            bookingId,
            from,
            to,
            actor,
            Timestamp.from(at),
        )
    }

    /** A completed refund of [amount] on [bookingId], completed at [at]. */
    protected fun refundCompletedAt(
        bookingId: Long,
        customerId: Long,
        amount: String,
        at: Instant,
    ) {
        val payment =
            jdbcTemplate.queryForObject(
                "INSERT INTO payment (customer_id, amount, subtotal, card_last4, cardholder_name) " +
                    "VALUES (?, ?, ?, '4242', 'Dash') RETURNING id",
                Long::class.java,
                customerId,
                BigDecimal(amount),
                BigDecimal(amount),
            )
        jdbcTemplate.update(
            "INSERT INTO refund (payment_id, booking_id, amount, reason, status, idempotency_key, " +
                "request_fingerprint, requested_by, created_at, completed_at) " +
                "VALUES (?, ?, ?, 'OTHER', 'COMPLETED', ?, 'f', ?, ?, ?)",
            payment,
            bookingId,
            BigDecimal(amount),
            "k-${System.nanoTime()}",
            customerId,
            Timestamp.from(at),
            Timestamp.from(at),
        )
    }

    protected fun summary(
        token: String,
        from: LocalDate,
        to: LocalDate,
    ): JsonNode = json(get(token, "/v1/admin/dashboard/summary?from=$from&to=$to"))

    protected fun get(
        token: String,
        url: String,
    ): MvcResult = mockMvc.get(url) { header("Authorization", "Bearer $token") }.andReturn()

    protected fun json(result: MvcResult): JsonNode =
        objectMapper.readTree(result.response.getContentAsString(Charsets.UTF_8))

    protected fun cacheCount(outcome: String): Double =
        meters.counter(
            "dbook.cache",
            "cache",
            "dashboard",
            "outcome",
            outcome,
        ).count()

    private companion object {
        val windows = AtomicInteger()
    }
}
