package com.dbook.application.getseatmapusecase

import com.dbook.application.GetSeatMapUseCase
import com.dbook.domain.Airline
import com.dbook.domain.Airport
import com.dbook.domain.Bookable
import com.dbook.domain.BookableRepository
import com.dbook.domain.Flight
import com.dbook.domain.Seat
import com.dbook.domain.SeatClass
import com.dbook.domain.SeatRepository
import java.math.BigDecimal
import java.time.LocalDateTime

class FakeBookableRepository(private val bookables: List<Bookable>) : BookableRepository {
    override fun findById(id: Long): Bookable? = bookables.find { it.id == id }
}

class FakeSeatRepository(private val seats: List<Seat>) : SeatRepository {
    override fun findById(id: Long): Seat? = seats.find { it.id == id }

    override fun findByBookableId(bookableId: Long): List<Seat> = seats.filter { it.bookableId == bookableId }

    override fun countAvailable(bookableId: Long): Int = 0

    override fun saveAll(seats: List<Seat>): List<Seat> =
        throw UnsupportedOperationException("not used by GetSeatMapUseCase")

    override fun reserve(seatId: Long): Seat = throw UnsupportedOperationException("not used by GetSeatMapUseCase")

    override fun release(seatId: Long): Seat = throw UnsupportedOperationException("not used by GetSeatMapUseCase")
}

// Shared "given": one Flight (bookableId) with a two-seat map.
abstract class GetSeatMapUseCaseFixture {
    protected val bookableId = 1L

    private val airline = Airline(id = 1, iataCode = "LA", name = "LATAM Airlines")
    private val origin =
        Airport(
            id = 1,
            iataCode = "GRU",
            name = "Guarulhos",
            city = "São Paulo",
            country = "Brasil",
            photoUrl = "https://example.com/photo.jpg",
        )
    private val destination =
        Airport(
            id = 2,
            iataCode = "GIG",
            name = "Galeão",
            city = "Rio de Janeiro",
            country = "Brasil",
            photoUrl = "https://example.com/photo.jpg",
        )
    private val flight =
        Flight(
            id = bookableId,
            title = "GRU-GIG",
            price = BigDecimal("500.00"),
            totalCapacity = 2,
            availableCapacity = 2,
            flightNumber = "DB1234",
            airline = airline,
            origin = origin,
            destination = destination,
            departureTime = LocalDateTime.of(2026, 10, 1, 8, 0),
            arrivalTime = LocalDateTime.of(2026, 10, 1, 9, 10),
            seatClass = SeatClass.ECONOMY,
        )

    protected val seats = listOf(Seat(1, bookableId, "1A"), Seat(2, bookableId, "1B"))
    protected val bookableRepository = FakeBookableRepository(listOf(flight))
    protected val seatRepository = FakeSeatRepository(seats)
    protected val useCase = GetSeatMapUseCase(bookableRepository, seatRepository)
}
