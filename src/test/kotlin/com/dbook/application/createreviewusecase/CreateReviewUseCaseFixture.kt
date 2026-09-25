package com.dbook.application.createreviewusecase

import com.dbook.application.CreateReviewUseCase
import com.dbook.domain.Airline
import com.dbook.domain.Airport
import com.dbook.domain.Booking
import com.dbook.domain.BookingRepository
import com.dbook.domain.BookingStatus
import com.dbook.domain.Flight
import com.dbook.domain.Review
import com.dbook.domain.ReviewRepository
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

class FakeReviewRepository : ReviewRepository {
    var nextId = 1L
    val saved = mutableListOf<Review>()

    override fun findById(id: Long): Review? = saved.find { it.id == id }

    override fun findByBookingId(bookingId: Long): Review? = saved.find { it.bookingId == bookingId }

    override fun save(review: Review): Review {
        val withId = Review(nextId++, review.bookingId, review.customerId, review.rating, review.comment)
        saved += withId
        return withId
    }
}

// Shared "given": ownerId has one CONFIRMED booking (reviewable) and one still-PENDING
// booking (not reviewable yet) — mirrors RegisterPaymentUseCaseFixture's two-bookings shape.
abstract class CreateReviewUseCaseFixture {
    protected val ownerId = 1L
    protected val confirmedBookingId = 100L
    protected val pendingBookingId = 101L

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
    private val flight =
        Flight(
            id = 1,
            title = "GRU-GIG",
            price = BigDecimal("500.00"),
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

    protected val bookingRepository =
        FakeBookingRepository(
            listOf(
                Booking(
                    confirmedBookingId,
                    flight,
                    seatId = 10,
                    customerId = ownerId,
                    status = BookingStatus.CONFIRMED,
                    paymentId = 500,
                ),
                Booking(pendingBookingId, flight, seatId = 11, customerId = ownerId),
            ),
        )
    protected val reviewRepository = FakeReviewRepository()
    protected val useCase = CreateReviewUseCase(bookingRepository, reviewRepository)
}
