package com.dbook.presentation.securityintegration

import kotlin.test.Test

class AdminRefreshFromAForeignOriginIsForbiddenTest : SecurityIntegrationFixture() {
    @Test
    fun `given a valid cookie when refreshing from another site then it returns 403 and the cookie is not spent`() {
        val email = uniqueEmail()
        registerStaff(email)
        val cookie = refreshCookieOf(adminLogin(email))

        adminRefresh(cookie, origin = "https://evil.example.com").andExpect { status { isForbidden() } }

        adminRefresh(cookie).andExpect { status { isOk() } }
    }
}
