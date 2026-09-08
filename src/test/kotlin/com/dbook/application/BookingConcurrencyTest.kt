package com.dbook.application

import com.dbook.domain.SeatClass
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

// Testa o lock otimista (item 2.2) contra o Postgres de verdade — precisa do
// `docker compose up -d` rodando. Testcontainers fica reservado pro M4 (CI),
// de propósito: não antecipamos essa peça aqui.
@SpringBootTest
class BookingConcurrencyTest {

	@Autowired
	lateinit var registerFlightUseCase: RegisterFlightUseCase

	@Autowired
	lateinit var registerBookingUseCase: RegisterBookingUseCase

	@Test
	fun `only one of two simultaneous bookings succeeds for the last seat`() {
		val flight = registerFlightUseCase.execute(
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
				} catch (ex: Exception) {
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

		assertEquals(1, successes.get(), "exatamente uma reserva deveria ter sido bem-sucedida")
		assertEquals(1, conflicts.get(), "a outra deveria ter falhado por conflito de versão")
	}
}
