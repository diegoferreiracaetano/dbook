package com.dbook.application.crm.getcustomerprofileusecase

import com.dbook.application.crm.CrmUseCaseFixture
import com.dbook.domain.audit.AuditContext
import com.dbook.domain.audit.AuditEntry
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.common.audit.AuditEvent
import kotlin.test.Test
import kotlin.test.assertTrue

class ReopeningWithinFiveMinutesIsOneAccessTest : CrmUseCaseFixture() {
    @Test
    fun `given a view recorded minutes ago when opening the profile again then no second record is written`() {
        val earlier = AuditEvent(support, AuditAction.CUSTOMER_VIEWED, customerId.toString())
        auditReader.entries = listOf(AuditEntry(1, now, earlier, AuditContext(null, null, null, null)))

        getProfile.execute(support, customerId)

        assertTrue(audit.events.isEmpty())
    }
}
