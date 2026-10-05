package com.dbook.presentation.twofactor

import kotlin.test.Test
import kotlin.test.assertEquals

class OnlyAnAccessTokenOpensAnEndpointTest : TwoFactorApiFixture() {
    private val email = uniqueEmail()

    @Test
    fun `given a challenge token when used as a bearer then it is a 401`() {
        staffWithTwoFactor(email)
        val challenge = challengeFor(email)

        assertEquals(401, getWith("/v1/admin/auth/me", challenge).response.status)
    }

    @Test
    fun `given the refresh token when used as a bearer then it is a 401`() {
        registerStaff(email)
        val refresh = loginRefreshToken(email, "s3cret-password")

        assertEquals(401, getWith("/v1/admin/auth/me", refresh).response.status)
    }

    @Test
    fun `given the access token when used as a bearer then it opens the endpoint`() {
        val access = registerStaffAndLogin(email)

        assertEquals(200, getWith("/v1/admin/auth/me", access).response.status)
    }
}
