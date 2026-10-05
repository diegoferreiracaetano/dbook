package com.dbook.presentation.catalogadmin

import kotlin.test.Test
import kotlin.test.assertEquals

class AirportsAreCreatedChangedAndRemovedOnlyIfUnusedTest : CatalogAdminFixture() {
    @Test
    fun `given an airport when created, changed and removed then it works, and one in use or duplicated is 409`() {
        val token = manager()

        fun bodyFor(code: String) =
            mapOf(
                "iataCode" to code,
                "name" to "Test Field",
                "city" to "Testville",
                "country" to "Brasil",
                "photoUrl" to "https://example.com/t.jpg",
                "region" to "América do Sul",
                "isPopular" to true,
            )

        // the code is random, so it can collide with an airport another test created: draw again until it is free
        val (code, created) =
            generateSequence { ('A'..'Z').shuffled().take(3).joinToString("") }
                .map { it to post(token, "/v1/admin/airports", bodyFor(it)) }
                .first { it.second.response.status != 409 }
        val body = bodyFor(code)
        assertEquals(201, created.response.status)
        val id = json(created)["id"].asLong()
        assertEquals(409, post(token, "/v1/admin/airports", body).response.status)
        assertEquals(400, post(token, "/v1/admin/airports", body + ("iataCode" to "gru1")).response.status)

        val changed = put(token, "/v1/admin/airports/$id", body + ("city" to "New City"))
        assertEquals("New City", json(changed)["city"].asText())
        assertEquals(204, delete(token, "/v1/admin/airports/$id").response.status)

        val gru = json(get(token, "/v1/admin/airports")).first { it["iataCode"].asText() == "GRU" }["id"].asLong()
        createFlight(token)
        assertEquals(409, delete(token, "/v1/admin/airports/$gru").response.status)
    }
}
