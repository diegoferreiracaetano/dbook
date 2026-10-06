package com.dbook.application.crm.getcustomerprofileusecase

import com.dbook.application.crm.CrmUseCaseFixture
import com.dbook.domain.common.audit.AuditAction
import kotlin.test.Test
import kotlin.test.assertEquals

class OpeningAProfileLeavesATrailTest : CrmUseCaseFixture() {
    @Test
    fun `given a profile never opened when opening it then the view is recorded`() {
        getProfile.execute(support, customerId)

        val event = audit.events.single()
        assertEquals(AuditAction.CUSTOMER_VIEWED, event.action)
        assertEquals(support, event.actor)
        assertEquals(customerId.toString(), event.targetId)
    }
}
