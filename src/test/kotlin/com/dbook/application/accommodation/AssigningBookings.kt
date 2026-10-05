package com.dbook.application.accommodation

import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.BookingRepository
import com.dbook.domain.booking.BookingStatus

/** Keeps bookings in memory and gives each new one an id, the way the table does. */
class AssigningBookings : BookingRepository {
    private val store = mutableMapOf<Long, Booking>()
    private var lastId = 0L

    override fun findById(id: Long): Booking? = store[id]

    override fun findByCustomerId(customerId: Long): List<Booking> = store.values.filter { it.customerId == customerId }

    override fun save(booking: Booking): Booking {
        val stored =
            booking.id?.let { booking }
                ?: Booking(
                    ++lastId, booking.bookable, booking.seatId, booking.customerId, booking.status, booking.paymentId,
                    booking.version, booking.price, booking.discount, booking.stay,
                )
        store[requireNotNull(stored.id)] = stored
        return stored
    }

    override fun countPending(): Long = store.values.count { it.status == BookingStatus.PENDING }.toLong()
}
