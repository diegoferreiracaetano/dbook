package com.dbook.infrastructure.persistence.booking

import com.dbook.domain.booking.BookingStatus
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.Clock

// Writes the booking's timeline. It is called by the booking repository on every status change, so that no use case,
// present or future, can change a status without leaving a trace. The actor is whoever is authenticated on the
// calling thread; there is none for the expiry job, which shows as "the system".
@Component
class BookingStatusHistoryWriter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val clock: Clock,
) {
    fun record(
        bookingId: Long,
        from: BookingStatus?,
        to: BookingStatus,
    ) {
        jdbc.update(
            "INSERT INTO booking_status_history (booking_id, from_status, to_status, actor_id, occurred_at) " +
                "VALUES (:booking, :from, :to, :actor, :at)",
            mapOf(
                "booking" to bookingId,
                "from" to from?.name,
                "to" to to.name,
                "actor" to currentActorId(),
                "at" to Timestamp.from(clock.instant()),
            ),
        )
    }

    private fun currentActorId(): Long? = SecurityContextHolder.getContext().authentication?.name?.toLongOrNull()
}
