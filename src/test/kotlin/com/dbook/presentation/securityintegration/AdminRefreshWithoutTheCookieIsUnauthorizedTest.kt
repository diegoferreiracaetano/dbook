package com.dbook.presentation.securityintegration

import kotlin.test.Test

class AdminRefreshWithoutTheCookieIsUnauthorizedTest : SecurityIntegrationFixture() {
    @Test
    fun `given no refresh cookie when refreshing from the portal then it returns 401`() {
        adminRefresh(cookieValue = null).andExpect { status { isUnauthorized() } }
    }
}
