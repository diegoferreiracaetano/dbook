package com.dbook.application.registerpaymentusecase

import com.dbook.application.RegisterPaymentUseCase
import com.dbook.domain.Airline
import com.dbook.domain.Airport
import com.dbook.domain.Booking
import com.dbook.domain.BookingRepository
import com.dbook.domain.Flight
import com.dbook.domain.Payment
import com.dbook.domain.PaymentRepository
import com.dbook.domain.SeatClass
import java.math.BigDecimal
import java.time.LocalDateTime

class FakeBookingRepository(initial: List<Booking>) : BookingRepository {
    val store = initial.associateBy { requireNotNull(it.id) }.toMutableMap()

    override fun findById(id: Long): Booking? = store[id]

    override fun findByCustomerId(customerId: Long): List<Booking> = store.values.filter { it.customerId == customerId }

    override fun save(booking: Booking): Booking {
        store[requireNotNull(booking.id)] = booking
        return booking
    }
}

class FakePaymentRepository : PaymentRepository {
    var nextId = 1L
    val saved = mutableListOf<Payment>()

    override fun save(payment: Payment): Payment {
        val withId = Payment(nextId++, payment.customerId, payment.amount, payment.cardLast4, payment.cardholderName)
        saved += withId
        return withId
    }
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
    protected val useCase = RegisterPaymentUseCase(bookingRepository, paymentRepository)
}
