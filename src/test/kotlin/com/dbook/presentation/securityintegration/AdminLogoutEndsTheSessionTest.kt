package com.dbook.presentation.securityintegration

import jakarta.servlet.http.Cookie
import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class AdminLogoutEndsTheSessionTest : SecurityIntegrationFixture() {
    @Test
    fun `given a session when logging out then the cookie is cleared and cannot be refreshed any more`() {
        val email = uniqueEmail()
        registerStaff(email)
        val cookie = refreshCookieOf(adminLogin(email))

        val logout =
            mockMvc.post("/v1/admin/auth/logout") {
                header("Origin", PORTAL_ORIGIN)
                cookie(Cookie("dbook_admin_refresh", cookie))
            }.andReturn()

        assertEquals(204, logout.response.status)
        assertContains(requireNotNull(logout.response.getHeader("Set-Cookie")), "Max-Age=0")
        assertEquals(401, adminRefresh(cookie).andReturn().response.status)
    }
}
