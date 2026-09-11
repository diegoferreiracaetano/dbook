package com.dbook.presentation.bookablecontroller

import com.dbook.domain.Seat
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.test.web.servlet.get

class ReturnsTheSeatMapTest : BookableControllerFixture() {
    @Test
    fun `given a bookable with seats when getting its seat map then it returns them`() {
        given(getSeatMapUseCase.execute(1)).willReturn(listOf(Seat(id = 1, bookableId = 1, label = "1A")))

        mockMvc.get("/bookables/1/seats").andExpect {
            status { isOk() }
            jsonPath("$[0].label") { value("1A") }
        }
    }
}
