package com.dbook.application.registerbookingusecase

import com.dbook.application.RegisterBookingCommand
import com.dbook.application.RegisterBookingUseCase
import com.dbook.domain.Airport
import com.dbook.domain.AvailabilityBroadcaster
import com.dbook.domain.Bookable
import com.dbook.domain.BookableRepository
import com.dbook.domain.Booking
import com.dbook.domain.BookingRepository
import com.dbook.domain.Flight
import com.dbook.domain.Seat
import com.dbook.domain.SeatClass
import com.dbook.domain.SeatNotFoundException
import com.dbook.domain.SeatRepository
import com.dbook.domain.SeatStatus
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.math.BigDecimal
import java.time.LocalDateTime

class FakeBookableRepository(private val bookables: List<Bookable>) : BookableRepository {
    override fun findById(id: Long): Bookable? = bookables.find { it.id == id }
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
        val seat = store[seatId] ?: throw SeatNotFoundException(seatId)
        check(seat.status == SeatStatus.AVAILABLE) { "Seat is not available: $seatId" }
        val reserved = Seat(seat.id, seat.bookableId, seat.label, SeatStatus.RESERVED)
        store[seatId] = reserved
        return reserved
    }

    override fun release(seatId: Long): Seat {
        val seat = store[seatId] ?: throw SeatNotFoundException(seatId)
        val released = Seat(seat.id, seat.bookableId, seat.label, SeatStatus.AVAILABLE)
        store[seatId] = released
        return released
    }
}

class FakeBookingRepository : BookingRepository {
    val saved = mutableListOf<Booking>()

    override fun findById(id: Long): Booking? = saved.find { it.id == id }

    override fun save(booking: Booking): Booking {
        val withId =
            if (booking.id == null) {
                Booking(saved.size + 1L, booking.bookable, booking.seatId, booking.customerId, booking.status)
            } else {
                booking
            }
        saved.removeAll { it.id == withId.id }
        saved += withId
        return withId
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

// Shared "given": one Flight (bookableId) with one AVAILABLE seat (seatId), plus a seat
// belonging to a different bookable (otherBookableSeatId) for the mismatch scenario.
abstract class RegisterBookingUseCaseFixture {
    protected val bookableId = 1L
    protected val seatId = 10L
    protected val otherBookableSeatId = 20L

    private val origin = Airport(id = 1, iataCode = "GRU", name = "Guarulhos", city = "São Paulo", country = "Brasil")
    private val destination =
        Airport(id = 2, iataCode = "GIG", name = "Galeão", city = "Rio de Janeiro", country = "Brasil")
    private val flight =
        Flight(
            id = bookableId,
            title = "GRU-GIG",
            price = BigDecimal("500.00"),
            totalCapacity = 1,
            availableCapacity = 1,
            flightNumber = "DB1234",
            origin = origin,
            destination = destination,
            departureTime = LocalDateTime.of(2026, 10, 1, 8, 0),
            arrivalTime = LocalDateTime.of(2026, 10, 1, 9, 10),
            seatClass = SeatClass.ECONOMY,
        )

    protected val bookableRepository = FakeBookableRepository(listOf(flight))
    protected val seatRepository =
        FakeSeatRepository(
            listOf(Seat(seatId, bookableId, "1A"), Seat(otherBookableSeatId, bookableId = 2L, label = "1A")),
        )
    protected val bookingRepository = FakeBookingRepository()
    protected val availabilityBroadcaster = RecordingAvailabilityBroadcaster()
    protected val useCase =
        RegisterBookingUseCase(bookableRepository, seatRepository, bookingRepository, availabilityBroadcaster)

    protected fun command(
        bookableIdOverride: Long = bookableId,
        seatIdOverride: Long = seatId,
    ) = RegisterBookingCommand(bookableId = bookableIdOverride, seatId = seatIdOverride, customerId = 1L)

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
