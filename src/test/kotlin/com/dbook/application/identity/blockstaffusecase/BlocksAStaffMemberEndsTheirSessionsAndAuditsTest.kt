package com.dbook.application.identity.blockstaffusecase

import com.dbook.application.identity.BlockStaffCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.common.audit.AuditAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlocksAStaffMemberEndsTheirSessionsAndAuditsTest : StaffUseCaseFixture() {
    @Test
    fun `given a staff member when blocking then they are blocked, their sessions end and the audit has the reason`() {
        val blocked = blockStaff.execute(BlockStaffCommand(actor, 2, " left the company "))

        assertTrue(blocked.isBlocked)
        assertEquals(listOf(2L), refreshTokens.revokedForUsers)
        val event = audit.events.single()
        assertEquals(AuditAction.STAFF_BLOCKED, event.action)
        assertEquals("left the company", event.reason)
        assertEquals("BLOCKED", event.after?.get("status"))
    }
}
