package com.dbook.presentation.customerrefund

import com.dbook.presentation.adminbookings.AdminBookingsFixture
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID

abstract class CustomerRefundFixture : AdminBookingsFixture() {
    protected fun policyOf(
        token: String,
        bookingId: Long,
    ): MvcResult =
        mockMvc.get("/v1/bookings/$bookingId/cancellation-policy") { header("Authorization", "Bearer $token") }
            .andReturn()

    protected fun refundRequest(
        token: String,
        bookingId: Long,
        key: String? = UUID.randomUUID().toString(),
    ): MvcResult =
        mockMvc.post("/v1/bookings/$bookingId/refund-request") {
            header("Authorization", "Bearer $token")
            key?.let { header("Idempotency-Key", it) }
        }.andReturn()

    protected fun refundRowsOf(bookingId: Long): Int =
        jdbcTemplate.queryForObject("SELECT count(*) FROM refund WHERE booking_id = ?", Int::class.java, bookingId) ?: 0
}
