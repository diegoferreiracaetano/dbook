package com.dbook.presentation.catalogadmin

import com.dbook.domain.common.access.Role
import com.dbook.presentation.securityintegration.SecurityIntegrationFixture
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.LocalDateTime

abstract class CatalogAdminFixture : SecurityIntegrationFixture() {
    protected fun manager(): String = registerStaffAndLogin(uniqueEmail(), Role.CATALOG_MANAGER)

    /** A date nobody else's test uses, so that a list filtered by it holds only this test's flights. */
    protected fun uniqueDeparture(): LocalDateTime =
        LocalDateTime.of(
            2040,
            1,
            1,
            8,
            0,
        ).plusDays((0..9000).random().toLong())

    protected fun flightBody(
        departure: LocalDateTime = uniqueDeparture(),
        capacity: Int = 12,
        aircraft: String = "Airbus A320",
        origin: String = "GRU",
        destination: String = "GIG",
    ): Map<String, Any?> =
        mapOf(
            "flightNumber" to "DBA${(10000..99999).random()}",
            "airlineIataCode" to "LA",
            "originIataCode" to origin,
            "destinationIataCode" to destination,
            "departureTime" to departure.toString(),
            "arrivalTime" to departure.plusHours(1).toString(),
            "seatClass" to "ECONOMY",
            "price" to 100.00,
            "totalCapacity" to capacity,
            "aircraftType" to aircraft,
        )

    protected fun createFlight(
        token: String,
        body: Map<String, Any?> = flightBody(),
    ): Long {
        val result =
            mockMvc.post("/v1/admin/flights") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(body)
            }.andReturn()
        return json(result)["id"].asLong()
    }

    protected fun json(result: MvcResult): JsonNode =
        objectMapper.readTree(result.response.getContentAsString(Charsets.UTF_8))

    protected fun flight(
        token: String,
        id: Long,
    ): MvcResult = mockMvc.get("/v1/admin/flights/$id") { header("Authorization", "Bearer $token") }.andReturn()

    protected fun versionOf(
        token: String,
        id: Long,
    ): Long = json(flight(token, id))["flight"]["version"].asLong()

    /** PUT with the flight's current data, changed by [changes], at [version] (the current one by default). */
    protected fun edit(
        token: String,
        id: Long,
        changes: Map<String, Any?> = emptyMap(),
        version: Long = versionOf(token, id),
    ): MvcResult {
        val current = json(flight(token, id))["flight"]
        val body =
            mutableMapOf<String, Any?>(
                "version" to version,
                "flightNumber" to current["flightNumber"].asText(),
                "airlineIataCode" to current["airlineIataCode"].asText(),
                "originIataCode" to current["origin"].asText(),
                "destinationIataCode" to current["destination"].asText(),
                "departureTime" to current["departureTime"].asText(),
                "arrivalTime" to current["arrivalTime"].asText(),
                "seatClass" to current["seatClass"].asText(),
                "price" to current["price"].asDouble(),
                "totalCapacity" to current["totalCapacity"].asInt(),
                "aircraftType" to current["aircraftType"].asText(),
            )
        body.putAll(changes)
        return mockMvc.put("/v1/admin/flights/$id") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }.andReturn()
    }

    /** A PUT with a complete, valid body for any id: for when the flight cannot (or need not) be read first. */
    protected fun editBlind(
        token: String,
        id: Long,
    ): MvcResult = put(token, "/v1/admin/flights/$id", flightBody() + ("version" to 0))

    protected fun cancelFlight(
        token: String,
        id: Long,
    ): MvcResult = mockMvc.post("/v1/admin/flights/$id/cancel") { header("Authorization", "Bearer $token") }.andReturn()

    protected fun searchFlights(
        token: String,
        vararg params: Pair<String, String>,
    ): MvcResult =
        mockMvc.get("/v1/admin/flights") {
            header("Authorization", "Bearer $token")
            params.forEach { (name, value) -> param(name, value) }
        }.andReturn()

    /** A booking attempt whose answer is kept, for the tests that expect it to be refused. */
    protected fun tryBook(
        token: String,
        bookableId: Long,
        seatId: Long,
    ): MvcResult = post(token, "/v1/bookings", mapOf("bookableId" to bookableId, "seatId" to seatId))

    protected fun seatLabels(id: Long): List<String> =
        jdbcTemplate.queryForList("SELECT label FROM seat WHERE bookable_id = ? ORDER BY id", String::class.java, id)

    protected fun seatIdOf(
        flightId: Long,
        label: String,
    ): Long =
        jdbcTemplate.queryForObject(
            "SELECT id FROM seat WHERE bookable_id = ? AND label = ?",
            Long::class.java,
            flightId,
            label,
        ) ?: error("no seat")

    protected fun post(
        token: String,
        url: String,
        body: Map<String, Any?>,
    ): MvcResult =
        mockMvc.post(url) {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }.andReturn()

    protected fun put(
        token: String,
        url: String,
        body: Map<String, Any?>,
    ): MvcResult =
        mockMvc.put(url) {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }.andReturn()

    protected fun get(
        token: String,
        url: String,
    ): MvcResult = mockMvc.get(url) { header("Authorization", "Bearer $token") }.andReturn()

    protected fun delete(
        token: String,
        url: String,
    ): MvcResult = mockMvc.delete(url) { header("Authorization", "Bearer $token") }.andReturn()
}
