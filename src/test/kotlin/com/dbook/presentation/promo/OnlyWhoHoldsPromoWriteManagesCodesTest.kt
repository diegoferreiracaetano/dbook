package com.dbook.presentation.promo

import com.dbook.domain.identity.Role
import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals

class OnlyWhoHoldsPromoWriteManagesCodesTest : PromoApiFixture() {
    @Test
    fun `given support, a customer and nobody when creating then 403, 403, 401, and the catalog manager 201`() {
        val support = registerStaffAndLogin(uniqueEmail(), Role.SUPPORT)
        val (customer) = customerWithABooking()

        assertEquals(403, createPromo(support).response.status)
        assertEquals(403, adminGet(support, "/v1/admin/promo-codes").response.status)
        assertEquals(403, createPromo(customer).response.status)
        assertEquals(401, mockMvc.get("/v1/admin/promo-codes").andReturn().response.status)
        assertEquals(201, createPromo(manager()).response.status)
    }
}
