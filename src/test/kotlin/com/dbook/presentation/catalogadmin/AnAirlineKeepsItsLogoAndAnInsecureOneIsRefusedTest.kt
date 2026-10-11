package com.dbook.presentation.catalogadmin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AnAirlineKeepsItsLogoAndAnInsecureOneIsRefusedTest : CatalogAdminFixture() {
    @Test
    fun `given an airline when its logo is set, changed and cleared then the listing shows it and http is a 400`() {
        val token = manager()
        val (code, created) =
            generateSequence { ('A'..'Z').shuffled().take(2).joinToString("") }
                .map {
                    it to
                        post(
                            token,
                            "/v1/admin/airlines",
                            mapOf("iataCode" to it, "name" to "Logo Air", "logoUrl" to "https://cdn.example.com/a.png"),
                        )
                }.first { it.second.response.status != 409 }
        assertEquals(201, created.response.status)
        assertEquals("https://cdn.example.com/a.png", json(created)["logoUrl"].asText())
        val id = json(created)["id"].asLong()

        val insecure = mapOf("iataCode" to code, "name" to "Logo Air", "logoUrl" to "http://cdn.example.com/a.png")
        assertEquals(400, put(token, "/v1/admin/airlines/$id", insecure).response.status)

        val cleared =
            put(token, "/v1/admin/airlines/$id", mapOf("iataCode" to code, "name" to "Logo Air", "logoUrl" to ""))
        assertNull(json(cleared)["logoUrl"].takeUnless { it.isNull })

        delete(token, "/v1/admin/airlines/$id")
    }
}
