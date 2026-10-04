package com.dbook.application.payment.registerpaymentusecase

import com.dbook.application.payment.RegisterPaymentCommand
import com.dbook.application.payment.RegisterPaymentUseCase
import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.BookingRepository
import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.catalog.Airline
import com.dbook.domain.catalog.Airport
import com.dbook.domain.catalog.Flight
import com.dbook.domain.catalog.SeatClass
import com.dbook.domain.payment.Payment
import com.dbook.domain.payment.PaymentRepository
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.math.BigDecimal
import java.time.LocalDateTime

class FakeBookingRepository(initial: List<Booking>) : BookingRepository {
    val store = initial.associateBy { requireNotNull(it.id) }.toMutableMap()

    override fun findById(id: Long): Booking? = store[id]

    override fun findByCustomerId(customerId: Long): List<Booking> = store.values.filter { it.customerId == customerId }

    override fun countPending(): Long = store.values.count { it.status == BookingStatus.PENDING }.toLong()

    override fun save(booking: Booking): Booking {
        store[requireNotNull(booking.id)] = booking
        return booking
    }
}

class FakePaymentRepository : PaymentRepository {
    var nextId = 1L
    val saved = mutableListOf<Payment>()

    override fun save(payment: Payment): Payment {
        val withId =
            Payment(
                id = nextId++,
                customerId = payment.customerId,
                amount = payment.amount,
                cardLast4 = payment.cardLast4,
                cardholderName = payment.cardholderName,
                idempotencyKey = payment.idempotencyKey,
                requestFingerprint = payment.requestFingerprint,
            )
        saved += withId
        return withId
    }

    override fun findByCustomerIdAndIdempotencyKey(
        customerId: Long,
        idempotencyKey: String,
    ): Payment? = saved.find { it.customerId == customerId && it.idempotencyKey == idempotencyKey }
}

// Shared "given": two PENDING bookings owned by ownerId, on two different flights
// (outbound $500 + return $300) — mirrors a Round Trip paid in one go.
abstract class RegisterPaymentUseCaseFixture {
    protected val ownerId = 1L
    protected val outboundBookingId = 100L
    protected val returnBookingId = 101L

    private val airline = Airline(id = 1, iataCode = "LA", name = "LATAM Airlines")
    private val gru =
        Airport(
            id = 1,
            iataCode = "GRU",
            name = "Guarulhos",
            city = "São Paulo",
            country = "Brasil",
            photoUrl = "https://example.com/photo.jpg",
            region = "América do Sul",
            isPopular = false,
        )
    private val gig =
        Airport(
            id = 2,
            iataCode = "GIG",
            name = "Galeão",
            city = "Rio de Janeiro",
            country = "Brasil",
            photoUrl = "https://example.com/photo.jpg",
            region = "América do Sul",
            isPopular = false,
        )

    private fun flight(
        id: Long,
        price: BigDecimal,
    ) = Flight(
        id = id,
        title = "GRU-GIG",
        price = price,
        totalCapacity = 1,
        availableCapacity = 0,
        flightNumber = "DB1234",
        airline = airline,
        origin = gru,
        destination = gig,
        departureTime = LocalDateTime.of(2026, 10, 1, 8, 0),
        arrivalTime = LocalDateTime.of(2026, 10, 1, 9, 10),
        seatClass = SeatClass.ECONOMY,
        aircraftType = "Airbus A320",
    )

    protected val outboundFlight = flight(id = 1, price = BigDecimal("500.00"))
    protected val returnFlight = flight(id = 2, price = BigDecimal("300.00"))

    protected val bookingRepository =
        FakeBookingRepository(
            listOf(
                Booking(outboundBookingId, outboundFlight, seatId = 10, customerId = ownerId),
                Booking(returnBookingId, returnFlight, seatId = 20, customerId = ownerId),
            ),
        )
    protected val paymentRepository = FakePaymentRepository()
    protected val meterRegistry = SimpleMeterRegistry()
    protected val useCase = RegisterPaymentUseCase(bookingRepository, paymentRepository, meterRegistry)

    // execute() registers an afterCommit callback, which needs an active transaction
    // synchronization even outside a real Spring transaction. This fakes just enough of it
    // and then runs the callbacks the way a successful commit would.
    protected fun executeCommitted(command: RegisterPaymentCommand): Payment {
        TransactionSynchronizationManager.initSynchronization()
        try {
            val payment = useCase.execute(command)
            TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }
            return payment
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    /** How many times the counter [name] was incremented for [outcome] (0 if it never was). */
    protected fun counted(
        name: String,
        outcome: String,
    ): Double = meterRegistry.find(name).tag("outcome", outcome).counter()?.count() ?: 0.0

    protected fun command(
        bookingIds: List<Long> = listOf(outboundBookingId),
        requestingUserId: Long = ownerId,
        idempotencyKey: String = "key-1",
    ) = RegisterPaymentCommand(bookingIds, "4242", "Jane Doe", requestingUserId, idempotencyKey)
}
