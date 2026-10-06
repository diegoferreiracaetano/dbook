package com.dbook.domain.audit.auditevent

import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.access.Role
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.common.audit.AuditEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class TheTargetTypeComesFromTheActionTest {
    @Test
    fun `given each action when the event is built then its target type is the one the action declares`() {
        AuditAction.entries.forEach { action ->
            val event = AuditEvent(Actor(1, Role.SUPER_ADMIN), action, targetId = "1")

            assertEquals(action.target, event.targetType)
        }
    }
}
