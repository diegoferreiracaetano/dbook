package com.dbook.application.accommodation.theendofabooking

import com.dbook.application.booking.cancelbookingusecase.CancelBookingUseCaseFixture
import com.dbook.domain.accommodation.Accommodation
import com.dbook.domain.accommodation.RoomType
import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.Stay
import com.dbook.domain.catalog.Airport
import com.dbook.domain.common.access.Role
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EndingAStayBookingGivesItsNightsBackTest : CancelBookingUseCaseFixture() {
    private val gig = Airport(2, "GIG", "Galeão", "Rio", "Brasil", "https://example.com/p.jpg", "América do Sul", true)
    private val hotel =
        Accommodation(
            1,
            "Copacabana",
            gig,
            "Av. Atlântica",
            4,
            roomTypes = listOf(RoomType(10, "Double", 2, BigDecimal("300.00"), 1)),
        )
    private val stay = Stay(10, LocalDate.of(2027, 1, 15), LocalDate.of(2027, 1, 17), 2, BigDecimal("300.00"))

    @Test
    fun `given a pending stay when its owner cancels it then the nights come back and no seat is touched`() {
        bookingRepository.save(Booking(200, hotel, null, ownerId, price = stay.total, stay = stay))
        roomInventory.reserve(stay)

        withTransactionSynchronization { useCase.execute(200, ownerId, Role.CLIENT) }

        assertEquals(listOf(stay), roomInventory.released)
        assertTrue(roomInventory.booked.values.all { it == 0 })
        assertEquals(null, availabilityBroadcaster.lastBookableId)
    }
}
