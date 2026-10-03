package com.dbook.application.booking.registerbookingusecase

import com.dbook.domain.catalog.BookableNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ThrowsWhenBookableDoesNotExistTest : RegisterBookingUseCaseFixture() {
    @Test
    fun `given a nonexistent bookableId when registering a booking then it throws BookableNotFoundException`() {
        assertFailsWith<BookableNotFoundException> {
            useCase.execute(command(bookableIdOverride = 999))
        }
    }
}
