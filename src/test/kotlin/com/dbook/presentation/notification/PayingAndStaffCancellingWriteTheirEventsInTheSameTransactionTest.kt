package com.dbook.presentation.notification

import com.dbook.presentation.customerprofile.CustomerProfileFixture
import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertEquals

class PayingAndStaffCancellingWriteTheirEventsInTheSameTransactionTest : CustomerProfileFixture() {
    private fun eventsOf(
        type: String,
        bookingId: Long,
    ): Int =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM outbox_event WHERE type = ? AND payload ->> 'bookingId' = ?",
            Int::class.java,
            type,
            bookingId.toString(),
        ) ?: 0

    @Test
    fun `given a booking when it is paid then a booking confirmed event exists for it`() {
        val token = registerAndLogin(uniqueEmail())
        val (booking) = bookSeats(token, 1)

        pay(token, booking)

        assertEquals(1, eventsOf("booking.confirmed", booking))
        assertEquals(0, eventsOf("booking.cancelled-by-staff", booking))
    }

    @Test
    fun `given bookings when staff cancels one and the owner another then only the staff one has an event`() {
        val owner = registerAndLogin(uniqueEmail())
        val staff = registerStaffAndLogin(uniqueEmail())
        val (byStaff, byOwner) = bookSeats(owner, 2)

        mockMvc.post("/v1/bookings/$byStaff/cancel") { header("Authorization", "Bearer $staff") }
        mockMvc.post("/v1/bookings/$byOwner/cancel") { header("Authorization", "Bearer $owner") }

        assertEquals(1, eventsOf("booking.cancelled-by-staff", byStaff))
        assertEquals(0, eventsOf("booking.cancelled-by-staff", byOwner))
    }
}
