package com.dbook.domain.catalog.airport

import com.dbook.domain.catalog.Airport
import com.dbook.domain.flight.Airline
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AnIataCodeHasTheRightShapeTest {
    @Test
    fun `given codes of the wrong shape when building an airport or an airline then they are refused`() {
        val photo = "https://example.com/a.jpg"
        listOf("gru", "GR", "GRUU", "G1U", "").forEach { code ->
            assertFailsWith<IllegalArgumentException>(code) { Airport(null, code, "N", "C", "B", photo, "R", false) }
        }
        listOf("la", "L", "LAT", "L-", "").forEach { code ->
            assertFailsWith<IllegalArgumentException>(code) { Airline(null, code, "Name") }
        }
        Airline(null, "G3", "Gol")
    }
}
