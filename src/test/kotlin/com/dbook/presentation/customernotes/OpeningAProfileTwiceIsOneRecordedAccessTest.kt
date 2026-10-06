package com.dbook.presentation.customernotes

import com.dbook.domain.common.access.Role
import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals

class OpeningAProfileTwiceIsOneRecordedAccessTest : CustomerNotesFixture() {
    @Test
    fun `given a support member opening the same profile twice when reading the audit then there is one view`() {
        val support = staffToken()
        val id = newCustomerId()

        repeat(2) {
            mockMvc.get("/v1/admin/customers/$id") { header("Authorization", "Bearer $support") }
                .andExpect { status { isOk() } }
        }

        val views = auditEntries(staffToken(Role.SUPER_ADMIN), "action=CUSTOMER_VIEWED&targetId=$id")
        assertEquals(1, views.size())
    }
}
