package com.dbook.presentation.promo

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class ACodeCreatedWithOnlyWhatItNeedsGetsItsDefaultsTest : PromoApiFixture() {
    @Test
    fun `given a body without the optional fields when a code is created then no minimum, one use each, no limit`() {
        val body =
            mapOf(
                "code" to aCode(),
                "type" to "FIXED",
                "value" to 15,
                "validFrom" to Instant.now().minus(1, ChronoUnit.DAYS).toString(),
                "validUntil" to Instant.now().plus(30, ChronoUnit.DAYS).toString(),
            )

        val created =
            mockMvc.post("/v1/admin/promo-codes") {
                header("Authorization", "Bearer ${manager()}")
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andReturn()

        assertEquals(201, created.response.status)
        assertEquals(1, json(created)["maxPerUser"].asInt())
        assertEquals(0.0, json(created)["minAmount"].asDouble())
        assertEquals(true, json(created)["maxRedemptions"].isNull)
    }
}
