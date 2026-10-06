package com.dbook.infrastructure.persistence.accommodation

import com.dbook.domain.accommodation.RoomInventory
import com.dbook.domain.accommodation.RoomUnavailableException
import com.dbook.domain.booking.Stay
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.sql.Date
import java.time.LocalDate

@Repository
class RoomInventoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : RoomInventory {
    // One statement per night: the row of that night is created if it is missing, and the count is raised only while it
    // is below the quantity. The row lock this takes is held to the end of the transaction, so a second stay on the
    // same night waits and then finds the count already raised. Nights go in order, so overlapping stays never wait
    // for each other in a circle. A full night changes no row, and the caller's exception undoes the nights before it.
    @Transactional(propagation = Propagation.MANDATORY)
    override fun reserve(stay: Stay) {
        stay.nightDates().forEach { night ->
            val taken =
                jdbc.update(
                    "INSERT INTO room_night (room_type_id, night, booked) VALUES (:type, :night, 1) " +
                        "ON CONFLICT (room_type_id, night) DO UPDATE SET booked = room_night.booked + 1 " +
                        "WHERE room_night.booked < (SELECT quantity FROM room_type WHERE id = :type)",
                    mapOf("type" to stay.roomTypeId, "night" to Date.valueOf(night)),
                )
            if (taken == 0) {
                throw RoomUnavailableException(night)
            }
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    override fun release(stay: Stay) {
        jdbc.update(
            "UPDATE room_night SET booked = booked - 1 " +
                "WHERE room_type_id = :type AND night >= :from AND night < :to AND booked > 0",
            mapOf("type" to stay.roomTypeId, "from" to Date.valueOf(stay.checkIn), "to" to Date.valueOf(stay.checkOut)),
        )
    }

    @Transactional(readOnly = true)
    override fun isAvailable(stay: Stay): Boolean =
        jdbc.queryForObject(
            "SELECT NOT EXISTS (SELECT 1 FROM generate_series(:from::date, :last::date, interval '1 day') " +
                "AS n(night) " +
                "LEFT JOIN room_night rn ON rn.room_type_id = :type AND rn.night = n.night::date " +
                "WHERE COALESCE(rn.booked, 0) >= (SELECT quantity FROM room_type WHERE id = :type))",
            mapOf(
                "type" to stay.roomTypeId, "from" to Date.valueOf(stay.checkIn),
                "last" to Date.valueOf(stay.checkOut.minusDays(1)),
            ),
            Boolean::class.javaObjectType,
        ) == true

    @Transactional(readOnly = true)
    override fun maxBookedFrom(
        roomTypeId: Long,
        from: LocalDate,
    ): Int =
        jdbc.queryForObject(
            "SELECT COALESCE(max(booked), 0) FROM room_night WHERE room_type_id = :type AND night >= :from",
            mapOf("type" to roomTypeId, "from" to Date.valueOf(from)),
            Int::class.javaObjectType,
        ) ?: 0
}
