package com.dbook.application.getseatmapusecase

import kotlin.test.Test
import kotlin.test.assertEquals

class ReturnsTheSeatMapOfABookableTest : GetSeatMapUseCaseFixture() {
    @Test
    fun `given a bookable with two seats when getting its seat map then it returns both seats`() {
        val seatMap = useCase.execute(bookableId)

        assertEquals(seats, seatMap)
    }
}
