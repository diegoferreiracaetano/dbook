package com.dbook.presentation.securityintegration

import kotlin.test.Test

class AdminRefreshWithoutAnOriginIsForbiddenTest : SecurityIntegrationFixture() {
    @Test
    fun `given a valid cookie when refreshing with no Origin header then it returns 403`() {
        val email = uniqueEmail()
        registerStaff(email)
        val cookie = refreshCookieOf(adminLogin(email))

        adminRefresh(cookie, origin = null).andExpect { status { isForbidden() } }
    }
}
