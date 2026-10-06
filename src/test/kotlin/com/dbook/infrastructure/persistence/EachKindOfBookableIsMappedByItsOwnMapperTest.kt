package com.dbook.infrastructure.persistence

import com.dbook.domain.accommodation.Accommodation
import com.dbook.domain.flight.Flight
import com.dbook.infrastructure.persistence.catalog.BookableJpaRepository
import com.dbook.infrastructure.persistence.catalog.BookableMappers
import com.dbook.presentation.accommodation.HotelApiFixture
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// The catalog does not know which kinds of Bookable exist: each kind registers a mapper. A mixed list comes back in
// the order it was asked for, each one made by the mapper of its kind.
class EachKindOfBookableIsMappedByItsOwnMapperTest : HotelApiFixture() {
    @Autowired
    lateinit var bookables: BookableJpaRepository

    @Autowired
    lateinit var mappers: BookableMappers

    @Test
    fun `given a flight and a hotel when mapped together then each comes back as its kind, in the order asked`() {
        val (flightId, _) = registerFlightWithOneSeat()
        val hotel = aHotel()
        val entities = listOf(hotel.id, flightId, hotel.id).map { bookables.findById(it).get() }

        val mapped = mappers.toDomain(entities)

        assertEquals(3, mapped.size)
        assertIs<Accommodation>(mapped[0])
        val flight = assertIs<Flight>(mapped[1])
        assertEquals(1, flight.availableCapacity, "the flight's own mapper counts its free seats")
        assertIs<Accommodation>(mapped[2])
        assertEquals(hotel.id, mapped[2].id)
    }

    @Test
    fun `given a single bookable when mapped alone then it is the same as in a list`() {
        val (flightId, _) = registerFlightWithOneSeat()

        val alone = mappers.toDomain(bookables.findById(flightId).get())

        assertEquals(flightId, alone.id)
        assertEquals(1, alone.availableCapacity)
    }
}
