package com.dbook.application.favorite

import com.dbook.domain.catalog.Airline
import com.dbook.domain.catalog.Airport
import com.dbook.domain.catalog.AirportRepository
import com.dbook.domain.catalog.Flight
import com.dbook.domain.catalog.FlightRepository
import com.dbook.domain.catalog.SeatClass
import com.dbook.domain.favorite.Favorite
import com.dbook.domain.favorite.FavoriteAddResult
import com.dbook.domain.favorite.FavoriteRepository
import com.dbook.domain.favorite.FavoriteType
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class InMemoryFavorites : FavoriteRepository {
    val all = mutableListOf<Favorite>()

    override fun add(
        favorite: Favorite,
        limit: Int,
    ): FavoriteAddResult =
        when {
            all.any {
                same(
                    it,
                    favorite.userId,
                    favorite.type,
                    favorite.targetId,
                )
            } -> FavoriteAddResult.ALREADY_FAVORITE
            all.count { it.userId == favorite.userId } >= limit -> FavoriteAddResult.LIMIT_REACHED
            else -> FavoriteAddResult.ADDED.also { all += favorite }
        }

    override fun remove(
        userId: Long,
        type: FavoriteType,
        targetId: String,
    ): Boolean = all.removeAll { same(it, userId, type, targetId) }

    private fun same(
        favorite: Favorite,
        userId: Long,
        type: FavoriteType,
        targetId: String,
    ) = favorite.userId == userId && favorite.type == type && favorite.targetId == targetId
}

private val airline = Airline(id = 1, iataCode = "LA", name = "LATAM Airlines")

private fun airport(
    id: Long,
    code: String,
) = Airport(id, code, "Airport $code", "City $code", "Brasil", "https://example.com/p.jpg", "América do Sul", false)

class FakeAirports : AirportRepository {
    private val known = mapOf("GRU" to airport(1, "GRU"), "GIG" to airport(2, "GIG"))

    override fun findByIataCode(iataCode: String): Airport? = known[iataCode]

    override fun findById(id: Long): Airport? = error("not needed")

    override fun findAll(): List<Airport> = error("not needed")

    override fun save(airport: Airport): Airport = error("not needed")

    override fun delete(id: Long) = error("not needed")

    override fun flightCount(id: Long): Long = error("not needed")
}

class FakeFlights : FlightRepository {
    private val flight =
        Flight(
            id = 10, title = "GRU-GIG", price = BigDecimal("100.00"), totalCapacity = 1, availableCapacity = 1,
            flightNumber = "DB1", airline = airline, origin = airport(1, "GRU"), destination = airport(2, "GIG"),
            departureTime = LocalDateTime.of(2027, 1, 1, 8, 0), arrivalTime = LocalDateTime.of(2027, 1, 1, 9, 0),
            seatClass = SeatClass.ECONOMY, aircraftType = "A320",
        )

    override fun findById(id: Long): Flight? = flight.takeIf { it.id == id }

    override fun save(flight: Flight): Flight = error("not needed")

    override fun update(
        flight: Flight,
        expectedVersion: Long?,
    ): Flight = error("not needed")

    override fun search(
        originIataCode: String,
        destinationIataCode: String,
        date: LocalDate,
    ): List<Flight> = error("not needed")

    override fun findLowestPrice(
        destinationIataCode: String,
        from: LocalDate,
        to: LocalDate,
    ): BigDecimal? = error("not needed")

    override fun findActive(): List<Flight> = error("not needed")
}

// GRU and GIG are destinations and flight 10 exists; nothing else does. The clock is 2026-10-04 12:00.
abstract class FavoriteFixture {
    protected val now: Instant = Instant.parse("2026-10-04T12:00:00Z")
    protected val favorites = InMemoryFavorites()
    protected val add = AddFavoriteUseCase(favorites, FakeAirports(), FakeFlights(), Clock.fixed(now, ZoneOffset.UTC))
    protected val remove = RemoveFavoriteUseCase(favorites)
}
