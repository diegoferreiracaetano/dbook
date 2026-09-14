package com.dbook.application.cancelbookingusecase

import com.dbook.application.CancelBookingUseCase
import com.dbook.domain.Airline
import com.dbook.domain.Airport
import com.dbook.domain.AvailabilityBroadcaster
import com.dbook.domain.Booking
import com.dbook.domain.BookingRepository
import com.dbook.domain.Flight
import com.dbook.domain.Seat
import com.dbook.domain.SeatClass
import com.dbook.domain.SeatRepository
import com.dbook.domain.SeatStatus
import org.springframework.transaction.support.TransactionSynchronizationManager
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

class FakeSeatRepository(seats: List<Seat>) : SeatRepository {
    val store = seats.associateBy { requireNotNull(it.id) }.toMutableMap()

    override fun findById(id: Long): Seat? = store[id]

    override fun findByBookableId(bookableId: Long): List<Seat> = store.values.filter { it.bookableId == bookableId }

    override fun countAvailable(bookableId: Long): Int =
        store.values.count { it.bookableId == bookableId && it.status == SeatStatus.AVAILABLE }

    override fun saveAll(seats: List<Seat>): List<Seat> {
        seats.forEach { store[requireNotNull(it.id)] = it }
        return seats
    }

    override fun reserve(seatId: Long): Seat {
        val seat = requireNotNull(store[seatId])
        val reserved = Seat(seat.id, seat.bookableId, seat.label, SeatStatus.RESERVED)
        store[seatId] = reserved
        return reserved
    }

    override fun release(seatId: Long): Seat {
        val seat = requireNotNull(store[seatId])
        val released = Seat(seat.id, seat.bookableId, seat.label, SeatStatus.AVAILABLE)
        store[seatId] = released
        return released
    }
}

class RecordingAvailabilityBroadcaster : AvailabilityBroadcaster {
    var lastBookableId: Long? = null
    var lastAvailableCapacity: Int? = null

    override fun broadcast(
        bookableId: Long,
        availableCapacity: Int,
    ) {
        lastBookableId = bookableId
        lastAvailableCapacity = availableCapacity
    }
}

// Shared "given": a PENDING booking (bookingId), owned by ownerId, on a RESERVED seat (seatId).
abstract class CancelBookingUseCaseFixture {
    protected val bookableId = 1L
    protected val seatId = 10L
    protected val bookingId = 100L
    protected val ownerId = 1L

    private val airline = Airline(id = 1, iataCode = "LA", name = "LATAM Airlines")
    private val origin =
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
    private val destination =
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
    private val flight =
        Flight(
            id = bookableId,
            title = "GRU-GIG",
            price = BigDecimal("500.00"),
            totalCapacity = 1,
            availableCapacity = 0,
            flightNumber = "DB1234",
            airline = airline,
            origin = origin,
            destination = destination,
            departureTime = LocalDateTime.of(2026, 10, 1, 8, 0),
            arrivalTime = LocalDateTime.of(2026, 10, 1, 9, 10),
            seatClass = SeatClass.ECONOMY,
            aircraftType = "Airbus A320",
        )

    protected val seatRepository = FakeSeatRepository(listOf(Seat(seatId, bookableId, "1A", SeatStatus.RESERVED)))
    protected val bookingRepository = FakeBookingRepository(listOf(Booking(bookingId, flight, seatId, ownerId)))
    protected val availabilityBroadcaster = RecordingAvailabilityBroadcaster()
    protected val useCase = CancelBookingUseCase(bookingRepository, seatRepository, availabilityBroadcaster)

    // execute() calls afterCommit(), which needs an active transaction synchronization even
    // outside a real Spring transaction — this fakes just enough of it for a unit test.
    protected fun withTransactionSynchronization(block: () -> Unit) {
        TransactionSynchronizationManager.initSynchronization()
        try {
            block()
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }
}
