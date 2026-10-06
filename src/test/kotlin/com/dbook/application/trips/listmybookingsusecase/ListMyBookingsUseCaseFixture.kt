package com.dbook.application.trips.listmybookingsusecase

import com.dbook.application.review.createreviewusecase.FakeReviewRepository
import com.dbook.application.trips.ListMyBookingsUseCase
import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.BookingRepository
import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.catalog.Airport
import com.dbook.domain.flight.Airline
import com.dbook.domain.flight.Flight
import com.dbook.domain.flight.SeatClass
import com.dbook.domain.review.Review
import com.dbook.domain.seating.Seat
import com.dbook.domain.seating.SeatRepository
import com.dbook.domain.seating.SeatStatus
import java.math.BigDecimal
import java.time.LocalDateTime

class FakeBookingRepository(private val bookings: List<Booking>) : BookingRepository {
    override fun findById(id: Long): Booking? = bookings.find { it.id == id }

    override fun findByCustomerId(customerId: Long): List<Booking> = bookings.filter { it.customerId == customerId }

    override fun countPending(): Long = bookings.count { it.status == BookingStatus.PENDING }.toLong()

    override fun save(booking: Booking): Booking = error("not used by ListMyBookingsUseCase")
}

class FakeSeatRepository(seats: List<Seat>) : SeatRepository {
    private val store = seats.associateBy { requireNotNull(it.id) }

    override fun findById(id: Long): Seat? = store[id]

    override fun findByBookableId(bookableId: Long): List<Seat> = error("not used by ListMyBookingsUseCase")

    override fun countAvailable(bookableId: Long): Int = error("not used by ListMyBookingsUseCase")

    override fun saveAll(seats: List<Seat>): List<Seat> = error("not used by ListMyBookingsUseCase")

    override fun reserve(seatId: Long): Seat = error("not used by ListMyBookingsUseCase")

    override fun release(seatId: Long): Seat = error("not used by ListMyBookingsUseCase")

    override fun deleteAll(seatIds: List<Long>) = error("not needed for this test")
}

// Shared "given": two customers (ownerId, otherOwnerId), each with their own booked seat on
// the same flight (bookableId).
abstract class ListMyBookingsUseCaseFixture {
    protected val bookableId = 1L
    protected val ownerId = 1L
    protected val otherOwnerId = 2L
    protected val seatId = 10L
    protected val otherOwnerSeatId = 11L
    protected val bookingId = 100L
    protected val otherOwnerBookingId = 101L

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
    protected val flight =
        Flight(
            id = bookableId,
            title = "GRU-GIG",
            price = BigDecimal("500.00"),
            totalCapacity = 2,
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

    protected fun useCase(
        bookings: List<Booking> = emptyList(),
        seats: List<Seat> = emptyList(),
        reviews: List<Review> = emptyList(),
    ) = ListMyBookingsUseCase(FakeBookingRepository(bookings), FakeSeatRepository(seats), FakeReviewRepository(reviews))

    protected val ownBooking = Booking(bookingId, flight, seatId, ownerId)
    protected val ownSeat = Seat(seatId, bookableId, "1A", SeatStatus.RESERVED)

    protected val otherOwnerBooking = Booking(otherOwnerBookingId, flight, otherOwnerSeatId, otherOwnerId)
    protected val otherOwnerSeat = Seat(otherOwnerSeatId, bookableId, "1B", SeatStatus.RESERVED)
}
