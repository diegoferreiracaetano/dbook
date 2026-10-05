package com.dbook.application.accommodation.registerstaybookingusecase

import com.dbook.application.accommodation.RegisterStayBookingCommand
import com.dbook.application.accommodation.RegisterStayBookingUseCase
import com.dbook.application.accommodation.StayFixture
import com.dbook.domain.booking.BookingEvents
import com.dbook.domain.booking.BookingStatus
import java.math.BigDecimal
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ABookedStayFreezesItsPriceAndHoldsItsNightsTest : StayFixture() {
    private val useCase = RegisterStayBookingUseCase(accommodations, inventory, bookings, outbox, clock)

    private fun command(
        checkIn: String = "2027-01-15",
        checkOut: String = "2027-01-18",
        guests: Int = 2,
    ) = RegisterStayBookingCommand(1, 10, day(checkIn), day(checkOut), guests, customerId = 7)

    @Test
    fun `given a free room when booked for three nights then it is PENDING, priced right, and the nights are held`() {
        val booking = useCase.execute(command())

        assertEquals(BookingStatus.PENDING, booking.status)
        assertNull(booking.seatId)
        assertEquals(BigDecimal("900.00"), booking.price)
        assertEquals(BigDecimal("300.00"), booking.stay!!.nightlyRate)
        assertEquals(3, booking.stay!!.nights)
        assertEquals(
            setOf(day("2027-01-15"), day("2027-01-16"), day("2027-01-17")),
            inventory.booked.keys.map {
                it.second
            }.toSet(),
        )
    }

    @Test
    fun `given a booked stay when it is made then an expiration is scheduled 15 minutes out, like for a seat`() {
        val booking = useCase.execute(command())

        val event = outbox.events.single()
        assertEquals(BookingEvents.EXPIRATION_REQUESTED, event.type)
        assertEquals(booking.id, event.payload["bookingId"])
        assertEquals(now.plus(Duration.ofMinutes(15)), event.availableAt)
    }

    @Test
    fun `given a booking when the rate changes afterwards then the booking keeps the rate it froze`() {
        val booking = useCase.execute(command())

        accommodations.save(hotel.withRoomTypes(listOf(double.copy(nightlyRate = BigDecimal("999.00")))))

        assertEquals(BigDecimal("900.00"), bookings.findById(booking.id!!)!!.price)
    }
}
