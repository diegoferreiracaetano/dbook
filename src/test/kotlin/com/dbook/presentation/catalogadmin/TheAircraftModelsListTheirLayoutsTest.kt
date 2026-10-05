package com.dbook.presentation.catalogadmin

import kotlin.test.Test
import kotlin.test.assertEquals

class TheAircraftModelsListTheirLayoutsTest : CatalogAdminFixture() {
    @Test
    fun `given the known aircraft when listing the models then each has its layout and seats per row`() {
        val models = json(get(manager(), "/v1/admin/aircraft-models"))

        val boeing = models.first { it["name"].asText() == "Boeing 777" }
        assertEquals(listOf(3, 4, 3), boeing["seatLayout"].map { it.asInt() })
        assertEquals(10, boeing["seatsPerRow"].asInt())
        assertEquals(3, models.size())
    }
}
