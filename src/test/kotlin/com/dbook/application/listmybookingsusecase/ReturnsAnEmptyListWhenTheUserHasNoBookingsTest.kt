package com.dbook.application.listmybookingsusecase

import kotlin.test.Test
import kotlin.test.assertEquals

class ReturnsAnEmptyListWhenTheUserHasNoBookingsTest : ListMyBookingsUseCaseFixture() {
    @Test
    fun `given a customer with no bookings when listing then it returns an empty list`() {
        val result = useCase().execute(ownerId)

        assertEquals(emptyList(), result)
    }
}
