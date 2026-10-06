package com.dbook.application.identity.changestaffroleusecase

import com.dbook.application.identity.ChangeStaffRoleCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.common.access.Role
import com.dbook.domain.common.audit.AuditAction
import kotlin.test.Test
import kotlin.test.assertEquals

class AuditsTheRoleChangeWithBothStatesTest : StaffUseCaseFixture() {
    @Test
    fun `given a role change when the audit is read then it has the old and the new role`() {
        changeRole.execute(ChangeStaffRoleCommand(actor, 2, Role.CATALOG_MANAGER))

        val event = audit.events.single()
        assertEquals(AuditAction.STAFF_ROLE_CHANGED, event.action)
        assertEquals("2", event.targetId)
        assertEquals("SUPPORT", event.before?.get("role"))
        assertEquals("CATALOG_MANAGER", event.after?.get("role"))
    }
}
