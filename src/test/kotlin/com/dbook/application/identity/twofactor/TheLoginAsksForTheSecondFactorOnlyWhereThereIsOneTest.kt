package com.dbook.application.identity.twofactor

import com.dbook.application.identity.StaffLoginOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TheLoginAsksForTheSecondFactorOnlyWhereThereIsOneTest : TwoFactorFixture() {
    @Test
    fun `given no second factor and none required when staff sign in then the session opens with the password`() {
        assertIs<StaffLoginOutcome.Session>(staffLogin.execute(staffPassword()))
        assertEquals(1.0, meters.counter("dbook.auth.login", "outcome", "success", "audience", "staff").count())
    }

    @Test
    fun `given an active second factor when staff sign in then a challenge comes back and no session`() {
        enrolled()

        val outcome = staffLogin.execute(staffPassword())

        assertEquals(StaffLoginOutcome.Challenge("challenge-VERIFY-1", enrollmentRequired = false), outcome)
        assertEquals(0, refreshTokens.saved.size, "no session was issued before the code")
        assertEquals(0.0, meters.counter("dbook.auth.login", "outcome", "success", "audience", "staff").count())
    }

    @Test
    fun `given an enrollment started and not confirmed when staff sign in then it protects nothing yet`() {
        enroll.execute(1)

        assertIs<StaffLoginOutcome.Session>(staffLogin.execute(staffPassword()))
    }
}
