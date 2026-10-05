package com.dbook.domain.booking

import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val MAX_NIGHTS = 30L

/**
 * What a booking of a room is, instead of a seat: the room type, the nights from [checkIn] (the first night) to
 * [checkOut] (the day the guests leave, not a night), how many guests, and the nightly rate **as it was when the
 * booking was made** (like the booking's price, it does not follow later rate changes).
 */
data class Stay(
    val roomTypeId: Long,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val guests: Int,
    val nightlyRate: BigDecimal,
) {
    init {
        require(checkOut.isAfter(checkIn)) { "checkOut must be after checkIn" }
        require(nights <= MAX_NIGHTS) { "a stay is at most $MAX_NIGHTS nights" }
        require(guests >= 1) { "guests must be at least 1" }
        require(nightlyRate > BigDecimal.ZERO) { "nightlyRate must be positive" }
    }

    val nights: Long get() = ChronoUnit.DAYS.between(checkIn, checkOut)

    /** The nights the room is taken: from [checkIn] up to, and not including, [checkOut]. */
    fun nightDates(): List<LocalDate> =
        generateSequence(checkIn) {
            it.plusDays(1)
        }.takeWhile { it.isBefore(checkOut) }.toList()

    val total: BigDecimal get() = nightlyRate * BigDecimal(nights)
}
