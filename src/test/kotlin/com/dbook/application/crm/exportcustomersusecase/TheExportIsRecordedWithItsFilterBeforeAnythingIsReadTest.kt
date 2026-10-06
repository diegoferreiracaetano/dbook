package com.dbook.application.crm.exportcustomersusecase

import com.dbook.application.crm.ExportCustomersCommand
import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.access.Role
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.crm.CustomerFilter
import com.dbook.domain.crm.CustomerSort
import com.dbook.domain.identity.UserStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class TheExportIsRecordedWithItsFilterBeforeAnythingIsReadTest : ExportCustomersUseCaseFixture() {
    @Test
    fun `given an export when it is prepared then the filter is in the audit and nothing was read yet`() {
        useCase.execute(
            ExportCustomersCommand(
                Actor(1, Role.SUPER_ADMIN),
                CustomerFilter(text = " maria ", status = UserStatus.BLOCKED),
                CustomerSort(),
            ),
        )

        val event = audit.events.single()
        assertEquals(AuditAction.CUSTOMER_EXPORTED, event.action)
        assertEquals("maria", event.after?.get("text"))
        assertEquals("BLOCKED", event.after?.get("status"))
        assertEquals(0, exporter.calls)
    }
}
