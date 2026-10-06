package com.dbook.presentation.promo

import com.dbook.domain.common.access.Role
import com.dbook.presentation.adminbookings.AdminBookingsFixture
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

// A customer who booked (each booking is a 100.00 flight), the team's promo endpoints, and the checkout calls.
abstract class PromoApiFixture : AdminBookingsFixture() {
    protected fun manager(): String = registerStaffAndLogin(uniqueEmail(), Role.CATALOG_MANAGER)

    protected fun json(result: MvcResult): JsonNode =
        objectMapper.readTree(result.response.getContentAsString(Charsets.UTF_8))

    protected fun aCode() = "T" + UUID.randomUUID().toString().replace("-", "").take(CODE_LENGTH).uppercase()

    protected fun promoBody(
        code: String = aCode(),
        vararg changes: Pair<String, Any?>,
    ): Map<String, Any?> =
        mapOf(
            "code" to code, "type" to "PERCENT", "value" to 10, "minAmount" to 0,
            "validFrom" to Instant.now().minus(1, ChronoUnit.DAYS).toString(),
            "validUntil" to Instant.now().plus(30, ChronoUnit.DAYS).toString(),
            "maxRedemptions" to null, "maxPerUser" to 1,
        ) + changes

    protected fun createPromo(
        token: String,
        body: Map<String, Any?> = promoBody(),
    ): MvcResult =
        mockMvc.post("/v1/admin/promo-codes") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }.andReturn()

    /** Creates a code and returns its id and its (capitals) code. */
    protected fun newPromo(
        token: String,
        vararg changes: Pair<String, Any?>,
    ): Pair<Long, String> {
        val created = json(createPromo(token, promoBody(aCode(), *changes)))
        return created["id"].asLong() to created["code"].asText()
    }

    protected fun updatePromo(
        token: String,
        id: Long,
        body: Map<String, Any?>,
    ): MvcResult =
        mockMvc.put("/v1/admin/promo-codes/$id") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }.andReturn()

    protected fun adminGet(
        token: String,
        url: String,
    ): MvcResult = mockMvc.get(url) { header("Authorization", "Bearer $token") }.andReturn()

    protected fun adminPost(
        token: String,
        url: String,
    ): MvcResult = mockMvc.post(url) { header("Authorization", "Bearer $token") }.andReturn()

    /** The caller's token and a PENDING booking for 100.00. */
    protected fun customerWithABooking(): Pair<String, Long> {
        val token = registerAndLogin(uniqueEmail())
        return token to bookSeats(token, 1).single()
    }

    protected fun payWith(
        token: String,
        bookings: List<Long>,
        code: String?,
        key: String = UUID.randomUUID().toString(),
    ): MvcResult =
        mockMvc.post("/v1/payments") {
            header("Authorization", "Bearer $token")
            header("Idempotency-Key", key)
            contentType = MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(
                    mapOf(
                        "bookingIds" to bookings, "cardLast4" to "4242", "cardholderName" to "Jane Doe",
                        "promoCode" to code,
                    ),
                )
        }.andReturn()

    protected fun preview(
        token: String,
        code: String,
        bookings: List<Long>,
    ): MvcResult =
        mockMvc.post("/v1/promo-codes/validate") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("code" to code, "bookingIds" to bookings))
        }.andReturn()

    protected fun redeemed(id: Long): Int =
        jdbcTemplate.queryForObject("SELECT redeemed FROM promo_code WHERE id = ?", Int::class.java, id) ?: 0

    protected fun statusOfBooking(id: Long): String =
        jdbcTemplate.queryForObject("SELECT status FROM booking WHERE id = ?", String::class.java, id).orEmpty()

    private companion object {
        const val CODE_LENGTH = 10
    }
}
