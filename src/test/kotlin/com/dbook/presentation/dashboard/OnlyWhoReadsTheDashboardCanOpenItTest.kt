package com.dbook.presentation.dashboard

import com.dbook.domain.common.access.Role
import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals

class OnlyWhoReadsTheDashboardCanOpenItTest : DashboardFixture() {
    @Test
    fun `given support, a catalog manager and a customer when asking then 200, 200 and 403`() {
        val url = "/v1/admin/dashboard/summary"

        assertEquals(200, get(staff(Role.SUPPORT), url).response.status)
        assertEquals(200, get(staff(Role.CATALOG_MANAGER), url).response.status)
        assertEquals(403, get(registerAndLogin(uniqueEmail()), url).response.status)
        assertEquals(401, mockMvc.get(url).andReturn().response.status)
    }
}
