package com.dbook.infrastructure.persistence.booking

import com.dbook.domain.booking.ActiveBooking
import com.dbook.domain.booking.BookingOccupancy
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class BookingOccupancyAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : BookingOccupancy {
    override fun bookedSeatIds(bookableId: Long): Set<Long> =
        jdbc.queryForList(
            "SELECT DISTINCT seat_id FROM booking WHERE bookable_id = :id",
            mapOf("id" to bookableId),
            Long::class.javaObjectType,
        ).toSet()

    override fun activeBookingOwners(bookableId: Long): List<ActiveBooking> =
        jdbc.query(
            "SELECT id, customer_id FROM booking WHERE bookable_id = :id AND status IN ('PENDING', 'CONFIRMED') " +
                "ORDER BY id",
            mapOf("id" to bookableId),
        ) { rs, _ -> ActiveBooking(rs.getLong("id"), rs.getLong("customer_id")) }

    override fun activeBookings(bookableId: Long): Long =
        jdbc.queryForObject(
            "SELECT count(*) FROM booking WHERE bookable_id = :id AND status IN ('PENDING', 'CONFIRMED')",
            mapOf("id" to bookableId),
            Long::class.javaObjectType,
        ) ?: 0L
}
