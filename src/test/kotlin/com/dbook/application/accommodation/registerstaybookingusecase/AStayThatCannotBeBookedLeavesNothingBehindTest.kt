package com.dbook.application.accommodation.registerstaybookingusecase

import com.dbook.application.accommodation.RegisterStayBookingCommand
import com.dbook.application.accommodation.RegisterStayBookingUseCase
import com.dbook.application.accommodation.StayFixture
import com.dbook.domain.accommodation.AccommodationNotFoundException
import com.dbook.domain.accommodation.RoomTypeNotFoundException
import com.dbook.domain.accommodation.RoomUnavailableException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AStayThatCannotBeBookedLeavesNothingBehindTest : StayFixture() {
    private val useCase = RegisterStayBookingUseCase(accommodations, inventory, bookings, outbox, clock)

    private fun command(
        hotelId: Long = 1,
        room: Long = 10,
        checkIn: String = "2027-01-15",
        checkOut: String = "2027-01-18",
        guests: Int = 2,
    ) = RegisterStayBookingCommand(hotelId, room, day(checkIn), day(checkOut), guests, customerId = 7)

    @Test
    fun `given the only room taken on a night when a stay overlaps it then it is refused and holds nothing`() {
        useCase.execute(command(checkIn = "2027-01-17", checkOut = "2027-01-19"))

        assertFailsWith<RoomUnavailableException> {
            useCase.execute(
                command(checkIn = "2027-01-15", checkOut = "2027-01-18"),
            )
        }

        assertEquals(
            setOf(day("2027-01-17"), day("2027-01-18")),
            inventory.booked.filterValues {
                it > 0
            }.keys.map { it.second }.toSet(),
        )
        assertEquals(1, outbox.events.size)
    }

    @Test
    fun `given a stay that starts the day another one leaves when booked then both fit, checkout is not a night`() {
        useCase.execute(command(checkIn = "2027-01-15", checkOut = "2027-01-18"))

        useCase.execute(command(checkIn = "2027-01-18", checkOut = "2027-01-20"))

        assertEquals(5, inventory.booked.size)
    }

    @Test
    fun `given too many guests, a past date or bad dates when booking then it is a bad request`() {
        assertFailsWith<IllegalArgumentException> { useCase.execute(command(guests = 3)) }
        assertFailsWith<IllegalArgumentException> {
            useCase.execute(
                command(checkIn = "2026-10-03", checkOut = "2026-10-05"),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            useCase.execute(
                command(checkIn = "2027-01-15", checkOut = "2027-01-15"),
            )
        }
        assertFailsWith<IllegalArgumentException> { useCase.execute(command(guests = 0)) }
        assertTrue(inventory.booked.isEmpty())
    }

    @Test
    fun `given an unknown hotel, an off-sale one or a room type that is off sale when booking then it is refused`() {
        assertFailsWith<AccommodationNotFoundException> { useCase.execute(command(hotelId = 99)) }
        assertFailsWith<IllegalStateException> { useCase.execute(command(hotelId = 2, room = 12)) }
        assertFailsWith<RoomTypeNotFoundException> { useCase.execute(command(room = 11)) }
        assertFailsWith<RoomTypeNotFoundException> { useCase.execute(command(room = 999)) }
    }
}
