package com.dbook.presentation.adminbookings

import com.dbook.ControllablePaymentGateway
import com.dbook.domain.identity.Role
import com.dbook.presentation.customerprofile.CustomerProfileFixture
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime
import java.util.UUID

// A customer who booked and paid, a gateway the test can switch off, and the staff endpoints to refund and to look.
abstract class AdminBookingsFixture : CustomerProfileFixture() {
    @Autowired
    lateinit var gateway: ControllablePaymentGateway

    protected fun staff(role: Role = Role.SUPPORT): String = registerStaffAndLogin(uniqueEmail(), role)

    /** A paid (CONFIRMED) booking: returns the customer's token and the booking id. */
    protected fun paidBooking(): Pair<String, Long> {
        val token = registerAndLogin(uniqueEmail())
        val (booking) = bookSeats(token, 1)
        pay(token, booking)
        return token to booking
    }

    /** A paid booking on a flight that leaves in [hours] hours: inside or outside the refund window. */
    protected fun paidBookingDepartingIn(hours: Long): Pair<String, Long> {
        val token = registerAndLogin(uniqueEmail())
        val (bookableId, seatId) = registerFlightWithOneSeat()
        val booking = book(token, bookableId, seatId)
        jdbcTemplate.update(
            "UPDATE flight SET departure_time = ?, arrival_time = ? WHERE id = ?",
            LocalDateTime.now().plusHours(hours),
            LocalDateTime.now().plusHours(hours + 1),
            bookableId,
        )
        pay(token, booking)
        return token to booking
    }

    protected fun refund(
        staffToken: String,
        bookingId: Long,
        key: String = UUID.randomUUID().toString(),
        body: Map<String, Any?> = mapOf("reason" to "CUSTOMER_REQUEST"),
    ): MvcResult =
        mockMvc.post("/v1/admin/bookings/$bookingId/refund") {
            header("Authorization", "Bearer $staffToken")
            header("Idempotency-Key", key)
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }.andReturn()

    protected fun retry(
        staffToken: String,
        refundId: Long,
    ): MvcResult =
        mockMvc.post("/v1/admin/refunds/$refundId/retry") { header("Authorization", "Bearer $staffToken") }.andReturn()

    protected fun adminBooking(
        staffToken: String,
        bookingId: Long,
    ): MvcResult =
        mockMvc.get("/v1/admin/bookings/$bookingId") { header("Authorization", "Bearer $staffToken") }.andReturn()

    protected fun searchBookings(
        staffToken: String,
        vararg params: Pair<String, String>,
    ): MvcResult =
        mockMvc.get("/v1/admin/bookings") {
            header("Authorization", "Bearer $staffToken")
            params.forEach { (name, value) -> param(name, value) }
        }.andReturn()

    /** The customer's own list, "my trips". */
    protected fun myBookings(customerToken: String): MvcResult =
        mockMvc.get("/v1/bookings") { header("Authorization", "Bearer $customerToken") }.andReturn()

    protected fun seatStatusOf(bookingId: Long): String =
        jdbcTemplate.queryForObject(
            "SELECT s.status FROM seat s JOIN booking b ON b.seat_id = s.id WHERE b.id = ?",
            String::class.java,
            bookingId,
        ) ?: error("no seat")

    protected fun refundIdOf(result: MvcResult): Long = bodyOf(result)["id"].asLong()

    protected fun statusOfRefund(result: MvcResult): String = bodyOf(result)["status"].asText()
}
