package com.dbook.presentation.securityintegration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class AdminRefreshRotatesTheCookieTest : SecurityIntegrationFixture() {
    @Test
    fun `given a session when refreshing from the portal then a new access token and a new cookie come back`() {
        val email = uniqueEmail()
        registerStaff(email)
        val cookie = refreshCookieOf(adminLogin(email))

        val refreshed = adminRefresh(cookie).andReturn()

        assertEquals(200, refreshed.response.status)
        assertNotNull(objectMapper.readTree(refreshed.response.contentAsString)["accessToken"])
        assertNotEquals(cookie, refreshCookieOf(refreshed))
        assertEquals(401, adminRefresh(cookie).andReturn().response.status)
    }
}
