package com.dbook.presentation.catalogadmin

import kotlin.test.Test
import kotlin.test.assertEquals

class AnEditOnAStaleVersionIsA409WithItsCodeTest : CatalogAdminFixture() {
    @Test
    fun `given two editors with the same version when both save then the second is 409 STALE_VERSION`() {
        val token = manager()
        val id = createFlight(token)
        val version = versionOf(token, id)

        val first = edit(token, id, mapOf("price" to 120.0), version = version)
        val second = edit(token, id, mapOf("price" to 130.0), version = version)

        assertEquals(200, first.response.status)
        assertEquals(409, second.response.status)
        assertEquals("STALE_VERSION", errorCodeOf(second))
        assertEquals(120.0, json(flight(token, id))["flight"]["price"].asDouble())
    }
}
