package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TheBookingAndItsExpirationEventCommitTogetherTest : SecurityIntegrationFixture() {
    private fun eventsOf(bookingId: Long) =
        jdbcTemplate.queryForList(
            "SELECT type, payload::text AS payload, available_at, published_at FROM outbox_event " +
                "WHERE aggregate_type = 'booking' AND aggregate_id = ?",
            bookingId.toString(),
        )

    @Test
    fun `given a booking made over HTTP when reading the outbox then its expiration event is there, due in 15 min`() {
        val token = registerAndLogin(uniqueEmail())
        val (bookableId, seatId) = registerFlightWithOneSeat()
        val before = Instant.now()

        val bookingId = book(token, bookableId, seatId)

        val event = eventsOf(bookingId).single()
        assertEquals("booking.expiration.requested", event["type"])
        assertEquals("""{"bookingId": $bookingId}""", event["payload"])
        assertNull(event["published_at"], "the relay has not run: nothing was sent")
        val due = (event["available_at"] as Timestamp).toInstant()
        assertTrue(
            due.isAfter(before.plus(Duration.ofMinutes(14))) && due.isBefore(before.plus(Duration.ofMinutes(16))),
            "due at $due",
        )
    }

    @Test
    fun `given a booking that fails when it is refused then no expiration event is left behind`() {
        val token = registerAndLogin(uniqueEmail())
        val (bookableId, seatId) = registerFlightWithOneSeat()
        val first = book(token, bookableId, seatId)
        val eventsBefore = jdbcTemplate.queryForObject("SELECT count(*) FROM outbox_event", Int::class.java)

        val refused =
            mockMvc.post("/v1/bookings") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("bookableId" to bookableId, "seatId" to seatId))
            }.andReturn()

        assertEquals(409, refused.response.status)
        assertEquals(eventsBefore, jdbcTemplate.queryForObject("SELECT count(*) FROM outbox_event", Int::class.java))
        assertEquals(1, eventsOf(first).size)
    }
}
