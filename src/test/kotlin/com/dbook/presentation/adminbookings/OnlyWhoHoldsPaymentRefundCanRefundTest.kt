package com.dbook.presentation.adminbookings

import com.dbook.domain.common.access.Role
import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertEquals

class OnlyWhoHoldsPaymentRefundCanRefundTest : AdminBookingsFixture() {
    @Test
    fun `given a catalog manager, a customer or no key when refunding then 403, 403 and 400`() {
        val (customer, booking) = paidBooking()

        assertEquals(403, refund(staff(Role.CATALOG_MANAGER), booking).response.status)
        assertEquals(403, refund(customer, booking).response.status)
        val noKey =
            mockMvc.post("/v1/admin/bookings/$booking/refund") {
                header("Authorization", "Bearer ${staff()}")
                contentType = org.springframework.http.MediaType.APPLICATION_JSON
                content = "{\"reason\":\"OTHER\"}"
            }.andReturn()
        assertEquals(400, noKey.response.status)
    }
}
