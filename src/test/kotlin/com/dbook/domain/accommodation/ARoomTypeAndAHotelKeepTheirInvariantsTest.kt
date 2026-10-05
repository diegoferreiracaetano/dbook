package com.dbook.domain.accommodation

import com.dbook.domain.catalog.Airport
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ARoomTypeAndAHotelKeepTheirInvariantsTest {
    private val gig =
        Airport(2, "GIG", "Galeão", "Rio de Janeiro", "Brasil", "https://example.com/p.jpg", "América do Sul", true)

    private fun room(
        name: String = "Double",
        rate: String = "300.00",
        quantity: Int = 5,
        active: Boolean = true,
    ) = RoomType(name = name, capacity = 2, nightlyRate = BigDecimal(rate), quantity = quantity, active = active)

    private fun hotel(
        vararg rooms: RoomType,
        stars: Int = 4,
    ) = Accommodation(
        name = "Copacabana",
        destination = gig,
        address = "Av. Atlântica",
        stars = stars,
        roomTypes = rooms.toList(),
    )

    @Test
    fun `given room types when the hotel is built then its price is the lowest active rate and capacity the rooms`() {
        val hotel =
            hotel(room("Double", "300.00", 5), room("Suite", "800.00", 2), room("Cheap", "150.00", 3, active = false))

        assertEquals(BigDecimal("300.00"), hotel.price)
        assertEquals(10, hotel.totalCapacity)
        assertEquals(10, hotel.availableCapacity)
    }

    @Test
    fun `given a hotel with no room types when built then it is free and empty`() {
        assertEquals(BigDecimal.ZERO, hotel().price)
        assertEquals(0, hotel().totalCapacity)
    }

    @Test
    fun `given bad data when a room type or a hotel is made then it is refused`() {
        assertFailsWith<IllegalArgumentException> { room(name = " ") }
        assertFailsWith<IllegalArgumentException> { room(rate = "0") }
        assertFailsWith<IllegalArgumentException> { room(quantity = 0) }
        assertFailsWith<IllegalArgumentException> {
            RoomType(name = "x", capacity = 0, nightlyRate = BigDecimal.ONE, quantity = 1)
        }
        assertFailsWith<IllegalArgumentException> { hotel(stars = 0) }
        assertFailsWith<IllegalArgumentException> { hotel(stars = 6) }
        assertFailsWith<IllegalStateException> { hotel(room("Double"), room("double")) }
    }
}
