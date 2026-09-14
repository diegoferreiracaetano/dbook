package com.dbook.domain.seatlayout

import com.dbook.domain.seatLayoutFor
import kotlin.test.Test
import kotlin.test.assertEquals

class ReturnsTheRealLayoutForEachKnownAircraftTypeTest {
    @Test
    fun `given Embraer E195 when getting its layout then it is 2 plus 2`() {
        assertEquals(listOf(2, 2), seatLayoutFor("Embraer E195"))
    }

    @Test
    fun `given Airbus A320 when getting its layout then it is 3 plus 3`() {
        assertEquals(listOf(3, 3), seatLayoutFor("Airbus A320"))
    }

    @Test
    fun `given Boeing 777 when getting its layout then it is 3 plus 4 plus 3`() {
        assertEquals(listOf(3, 4, 3), seatLayoutFor("Boeing 777"))
    }

    @Test
    fun `given an unknown aircraft type when getting its layout then it defaults to 3 plus 3`() {
        assertEquals(listOf(3, 3), seatLayoutFor("Some Future Aircraft"))
    }
}
