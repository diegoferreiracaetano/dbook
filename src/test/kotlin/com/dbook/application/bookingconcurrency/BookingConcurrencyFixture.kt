package com.dbook.application.bookingconcurrency

import com.dbook.AbstractIntegrationTest
import com.dbook.application.CancelBookingUseCase
import com.dbook.application.RegisterBookingCommand
import com.dbook.application.RegisterBookingUseCase
import com.dbook.application.RegisterFlightCommand
import com.dbook.application.RegisterFlightUseCase
import com.dbook.application.RegisterPaymentCommand
import com.dbook.application.RegisterPaymentUseCase
import com.dbook.application.RegisterUserCommand
import com.dbook.application.RegisterUserUseCase
import com.dbook.domain.BookingRepository
import com.dbook.domain.SeatClass
import com.dbook.domain.SeatRepository
import com.dbook.domain.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// Shared setup for the concurrency scenarios: real use cases against the Testcontainers
// Postgres (item 4.2), so the optimistic locks (seat and booking) are exercised for real.
abstract class BookingConcurrencyFixture : AbstractIntegrationTest() {
    @Autowired
    lateinit var registerUserUseCase: RegisterUserUseCase

    @Autowired
    lateinit var registerFlightUseCase: RegisterFlightUseCase

    @Autowired
    lateinit var registerBookingUseCase: RegisterBookingUseCase

    @Autowired
    lateinit var registerPaymentUseCase: RegisterPaymentUseCase

    @Autowired
    lateinit var cancelBookingUseCase: CancelBookingUseCase

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var bookingRepository: BookingRepository

    @Autowired
    lateinit var seatRepository: SeatRepository

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    /** @return the id of a freshly registered user. */
    protected fun registerUser(): Long {
        val email = "race${(1..999_999_999).random()}@example.com"
        registerUserUseCase.execute(RegisterUserCommand(email, PASSWORD, "Race User"))
        return requireNotNull(requireNotNull(userRepository.findByEmail(email)).id)
    }

    /** @return the new flight's bookableId. */
    protected fun registerFlight(totalCapacity: Int): Long {
        val flight =
            registerFlightUseCase.execute(
                RegisterFlightCommand(
                    flightNumber = "DBC${(10000..99999).random()}",
                    airlineIataCode = "LA",
                    originIataCode = "GRU",
                    destinationIataCode = "EZE",
                    departureTime = LocalDateTime.of(2027, 4, 1, 8, 0),
                    arrivalTime = LocalDateTime.of(2027, 4, 1, 9, 10),
                    seatClass = SeatClass.ECONOMY,
                    price = BigDecimal("100.00"),
                    totalCapacity = totalCapacity,
                    aircraftType = "Airbus A320",
                ),
            )
        return requireNotNull(flight.id)
    }

    protected fun seatIdsOf(bookableId: Long): List<Long> =
        seatRepository.findByBookableId(bookableId).map { requireNotNull(it.id) }

    /** @return the id of a new PENDING booking for [seatId]. */
    protected fun bookSeat(
        bookableId: Long,
        seatId: Long,
        userId: Long,
    ): Long =
        requireNotNull(
            registerBookingUseCase.execute(RegisterBookingCommand(bookableId, seatId, userId)).id,
        )

    /** Pays [bookingId] as [userId]; repeating the same [idempotencyKey] is a retry of the same request. */
    protected fun pay(
        bookingId: Long,
        userId: Long,
        idempotencyKey: String = "key-1",
    ) = registerPaymentUseCase.execute(
        RegisterPaymentCommand(
            listOf(bookingId),
            cardLast4 = "4242",
            cardholderName = "Race User",
            requestingUserId = userId,
            idempotencyKey = idempotencyKey,
        ),
    )

    /**
     * Releases every operation at the same instant, each on its own thread.
     *
     * @return how many completed without throwing. The exact exception type of the losers
     * (an optimistic-lock failure, wrapped by Spring's proxy chain) isn't asserted on
     * purpose — the count already proves the concurrency-control property.
     */
    protected fun race(vararg operations: () -> Unit): Int = outcomesOf(operations).count { it == null }

    /** Like [race], but returns what each operation threw (null when it succeeded), in order. */
    protected fun raceOutcomes(vararg operations: () -> Unit): List<Throwable?> = outcomesOf(operations)

    private fun outcomesOf(operations: Array<out () -> Unit>): List<Throwable?> {
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(operations.size)
        val results =
            operations.map { operation ->
                executor.submit<Throwable?> {
                    start.await()
                    runCatching { operation() }.exceptionOrNull()
                }
            }
        start.countDown()
        val outcomes = results.map { it.get(10, TimeUnit.SECONDS) }
        executor.shutdown()
        return outcomes
    }

    private companion object {
        const val PASSWORD = "s3cret-password"
    }
}
