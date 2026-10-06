package com.dbook.application.identity.unblockstaffusecase

import com.dbook.application.identity.BlockStaffCommand
import com.dbook.application.identity.UnblockStaffCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.common.audit.AuditAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class UnblocksAndAuditsTest : StaffUseCaseFixture() {
    @Test
    fun `given a blocked staff member when unblocking then they are active again and the audit has it`() {
        blockStaff.execute(BlockStaffCommand(actor, 2, "left the company"))
        audit.events.clear()

        val unblocked = unblockStaff.execute(UnblockStaffCommand(actor, 2))

        assertFalse(unblocked.isBlocked)
        assertEquals(AuditAction.STAFF_UNBLOCKED, audit.events.single().action)
    }
}
