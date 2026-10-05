package com.dbook.presentation.pricing

import com.dbook.presentation.catalogadmin.CatalogAdminFixture
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.time.LocalDate

// A flight GRU-GIG on a date of its own (so the alerts of other tests never meet it) and the customer's alert calls.
abstract class PricingApiFixture : CatalogAdminFixture() {
    protected fun body(result: MvcResult): JsonNode =
        objectMapper.readTree(result.response.getContentAsString(Charsets.UTF_8))

    protected fun aCustomer(): Pair<String, Long> {
        val email = uniqueEmail()
        return registerAndLogin(email) to userIdOf(email)
    }

    protected fun history(
        flightId: Long,
        token: String? = null,
    ): MvcResult =
        mockMvc.get("/v1/flights/$flightId/price-history") { token?.let { header("Authorization", "Bearer $it") } }
            .andReturn()

    protected fun createAlert(
        token: String,
        date: LocalDate,
        target: Any = 300,
        origin: String = "GRU",
        destination: String = "GIG",
    ): MvcResult =
        mockMvc.post("/v1/price-alerts") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(
                    mapOf(
                        "origin" to origin, "destination" to destination, "date" to date.toString(),
                        "targetPrice" to target,
                    ),
                )
        }.andReturn()

    protected fun updateAlert(
        token: String,
        id: Long,
        changes: Map<String, Any?>,
    ): MvcResult =
        mockMvc.patch("/v1/price-alerts/$id") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(changes)
        }.andReturn()

    protected fun deleteAlert(
        token: String,
        id: Long,
    ): MvcResult = mockMvc.delete("/v1/price-alerts/$id") { header("Authorization", "Bearer $token") }.andReturn()

    protected fun myAlerts(token: String): MvcResult =
        mockMvc.get("/v1/price-alerts") { header("Authorization", "Bearer $token") }.andReturn()

    protected fun aFutureDate(): LocalDate = LocalDate.now().plusDays((60L..9_000L).random())

    protected fun outboxEvents(
        type: String,
        aggregateId: String,
    ): List<JsonNode> =
        jdbcTemplate.queryForList(
            "SELECT payload::text FROM outbox_event WHERE type = ? AND aggregate_id = ? ORDER BY created_at, id",
            String::class.java,
            type,
            aggregateId,
        ).map { objectMapper.readTree(it) }
}
