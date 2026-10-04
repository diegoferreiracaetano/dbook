package com.dbook.domain.audit.auditevent

import com.dbook.domain.audit.AuditAction
import com.dbook.domain.audit.AuditEvent
import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.Role
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
