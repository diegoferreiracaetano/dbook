package com.dbook.domain.audit.auditevent

import com.dbook.domain.audit.AuditAction
import com.dbook.domain.audit.AuditEvent
import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsABlankTargetIdTest {
    @Test
    fun `given a blank target id when building the event then it throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            AuditEvent(Actor(1, Role.SUPER_ADMIN), AuditAction.FLIGHT_CREATED, targetId = "  ")
        }
    }
}
