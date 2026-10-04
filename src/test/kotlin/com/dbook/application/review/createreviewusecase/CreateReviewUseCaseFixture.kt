package com.dbook.application.review.createreviewusecase

import com.dbook.application.review.CreateReviewUseCase
import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.BookingRepository
import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.catalog.Airline
import com.dbook.domain.catalog.Airport
import com.dbook.domain.catalog.Flight
import com.dbook.domain.catalog.SeatClass
import com.dbook.domain.review.Review
import com.dbook.domain.review.ReviewRepository
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

class FakeReviewRepository(
    reviews: List<Review> = emptyList(),
    private val averageRatingByDestination: Map<String, Double> = emptyMap(),
) : ReviewRepository {
    private val store = reviews.associateBy { it.bookingId }.toMutableMap()

    override fun findById(id: Long): Review? = store.values.find { it.id == id }

    override fun findByBookingId(bookingId: Long): Review? = store[bookingId]

    override fun save(review: Review): Review {
        store[review.bookingId] = review
        return review
    }

    override fun findAverageRatingByDestination(destinationIataCode: String): Double? =
        averageRatingByDestination[destinationIataCode]
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
