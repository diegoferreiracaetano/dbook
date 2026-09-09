package com.dbook.application

import com.dbook.AbstractIntegrationTest
import com.dbook.domain.SeatClass
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

// Exercises the optimistic lock (item 2.2) against a real Postgres, provisioned by
// Testcontainers (item 4.2) — no manual `docker compose up -d` needed anymore.
class BookingConcurrencyTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var registerFlightUseCase: RegisterFlightUseCase

    @Autowired
    lateinit var registerBookingUseCase: RegisterBookingUseCase

    @Test
    fun `only one of two simultaneous bookings succeeds for the last seat`() {
        val flight =
            registerFlightUseCase.execute(
                RegisterFlightCommand(
                    flightNumber = "DBC${(10000..99999).random()}",
                    originIataCode = "GRU",
                    destinationIataCode = "GIG",
                    departureTime = LocalDateTime.of(2026, 12, 1, 8, 0),
                    arrivalTime = LocalDateTime.of(2026, 12, 1, 9, 10),
                    seatClass = SeatClass.ECONOMY,
                    price = BigDecimal("100.00"),
                    totalCapacity = 1,
                ),
            )

        val successes = AtomicInteger(0)
        val conflicts = AtomicInteger(0)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val done = CountDownLatch(2)
        val executor = Executors.newFixedThreadPool(2)

        repeat(2) {
            executor.submit {
                ready.countDown()
                start.await()
                try {
                    registerBookingUseCase.execute(RegisterBookingCommand(flight.id!!, customerId = 1L))
                    successes.incrementAndGet()
                } catch (
                    @Suppress("SwallowedException")
                    ex: Exception,
                ) {
                    // exact exception type (ObjectOptimisticLockingFailureException, wrapped
                    // by Spring's proxy chain) isn't asserted on purpose — the count-based
                    // assertions below already prove the concurrency-control property.
                    conflicts.incrementAndGet()
                } finally {
                    done.countDown()
                }
            }
        }

        ready.await()
        start.countDown()
        done.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        assertEquals(1, successes.get(), "exactly one booking should have succeeded")
        assertEquals(1, conflicts.get(), "the other one should have failed with a version conflict")
    }
}
