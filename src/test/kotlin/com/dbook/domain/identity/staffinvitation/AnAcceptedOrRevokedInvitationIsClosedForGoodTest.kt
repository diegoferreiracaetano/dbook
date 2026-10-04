package com.dbook.domain.identity.staffinvitation

import kotlin.test.Test
import kotlin.test.assertFailsWith

class AnAcceptedOrRevokedInvitationIsClosedForGoodTest : StaffInvitationFixture() {
    @Test
    fun `given an accepted and a revoked invitation when changed again then every attempt throws`() {
        val accepted = pending().accept(now)
        val revoked = pending().revoke(now)

        assertFailsWith<IllegalStateException> { accepted.revoke(now) }
        assertFailsWith<IllegalStateException> { accepted.reissue("hash", now) }
        assertFailsWith<IllegalStateException> { revoked.accept(now) }
        assertFailsWith<IllegalStateException> { revoked.reissue("hash", now) }
    }
}
