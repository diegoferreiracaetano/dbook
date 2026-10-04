package com.dbook.presentation.seating.bookablecontroller

import com.dbook.domain.catalog.BookableNotFoundException
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.test.web.servlet.get

class ReturnsNotFoundWhenBookableDoesNotExistTest : BookableControllerFixture() {
    @Test
    fun `given a nonexistent bookable when getting its seat map then it returns 404`() {
        given(getSeatMapUseCase.execute(999)).willThrow(BookableNotFoundException(999))

        mockMvc.get("/v1/bookables/999/seats").andExpect {
            status { isNotFound() }
        }
    }
}
