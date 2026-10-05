package com.dbook.presentation.catalogadmin

import kotlin.test.Test
import kotlin.test.assertEquals

class AirlinesAreCreatedRenamedAndRemovedOnlyIfUnusedTest : CatalogAdminFixture() {
    @Test
    fun `given an airline when created, renamed and removed then it works, and one in use or duplicated is 409`() {
        val token = manager()
        // the code is random, so it can collide with an airline another test created: draw again until it is free
        val (code, created) =
            generateSequence { ('A'..'Z').shuffled().take(2).joinToString("") }
                .map { it to post(token, "/v1/admin/airlines", mapOf("iataCode" to it, "name" to "Test Air")) }
                .first { it.second.response.status != 409 }
        val body = mapOf("iataCode" to code, "name" to "Test Air")
        assertEquals(201, created.response.status)
        val id = json(created)["id"].asLong()
        assertEquals(409, post(token, "/v1/admin/airlines", body).response.status)
        assertEquals(400, post(token, "/v1/admin/airlines", mapOf("iataCode" to "x", "name" to "Bad")).response.status)

        val renamed = put(token, "/v1/admin/airlines/$id", mapOf("iataCode" to code, "name" to "Renamed Air"))
        assertEquals("Renamed Air", json(renamed)["name"].asText())
        assertEquals(true, json(get(token, "/v1/admin/airlines")).any { it["iataCode"].asText() == code })
        assertEquals(204, delete(token, "/v1/admin/airlines/$id").response.status)
        assertEquals(404, delete(token, "/v1/admin/airlines/$id").response.status)

        val latam = json(get(token, "/v1/admin/airlines")).first { it["iataCode"].asText() == "LA" }["id"].asLong()
        createFlight(token)
        assertEquals(409, delete(token, "/v1/admin/airlines/$latam").response.status)
    }
}
