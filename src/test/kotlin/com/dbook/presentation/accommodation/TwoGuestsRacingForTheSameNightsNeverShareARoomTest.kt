package com.dbook.presentation.accommodation

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals

class TwoGuestsRacingForTheSameNightsNeverShareARoomTest : HotelApiFixture() {
    private fun race(
        vararg stays: Pair<Long, Long>,
        hotel: Hotel,
    ): List<Int> {
        val tokens = stays.map { aCustomerToken() }
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(stays.size)
        val statuses =
            stays.zip(tokens).map { (stay, token) ->
                pool.submit<Int> {
                    start.await()
                    bookStay(token, hotel, day(stay.first), day(stay.second)).response.status
                }
            }.also { start.countDown() }.map { it.get() }
        pool.shutdown()
        return statuses
    }

    @Test
    fun `given one room when two guests book the same nights at once then one wins and the other gets a 409`() {
        val hotel = aHotel()

        val statuses = race(0L to 3L, 0L to 3L, hotel = hotel)

        assertEquals(listOf(201, 409), statuses.sorted())
        assertEquals(3, bookedNightsOf(hotel.roomTypeId))
    }

    @Test
    fun `given one room when three stays overlapping in pairs race then no night is ever held twice`() {
        val hotel = aHotel()

        val statuses = race(0L to 3L, 2L to 5L, 4L to 6L, hotel = hotel)

        val tooMany =
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM room_night WHERE room_type_id = ? AND booked > 1",
                Int::class.java,
                hotel.roomTypeId,
            )
        assertEquals(0, tooMany)
        val won = statuses.count { it == 201 }
        assertEquals(true, won in 1..2)
        assertEquals(true, statuses.all { it == 201 || it == 409 })
        val bookedNights = bookedNightsOf(hotel.roomTypeId)
        val confirmedNights =
            jdbcTemplate.queryForObject(
                "SELECT COALESCE(sum(check_out - check_in), 0) FROM booking WHERE room_type_id = ?",
                Int::class.java,
                hotel.roomTypeId,
            )
        assertEquals(confirmedNights, bookedNights)
    }
}
