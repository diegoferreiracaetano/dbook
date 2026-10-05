package com.dbook.application.crm.getcustomerprofileusecase

import com.dbook.application.crm.CrmUseCaseFixture
import com.dbook.application.crm.GetCustomerProfileUseCase
import com.dbook.domain.audit.AuditAction
import kotlin.test.Test
import kotlin.test.assertEquals

class TheDedupWindowIsTheLastFiveMinutesOfThatActorAndCustomerTest : CrmUseCaseFixture() {
    @Test
    fun `given a view when checking for a recent one then it asks for the same actor, customer and 5 minutes`() {
        getProfile.execute(support, customerId)

        val filter = requireNotNull(auditReader.lastFilter)
        assertEquals(support.id, filter.actorId)
        assertEquals(AuditAction.CUSTOMER_VIEWED, filter.action)
        assertEquals(customerId.toString(), filter.targetId)
        assertEquals(now.minus(GetCustomerProfileUseCase.SAME_ACCESS_WINDOW), filter.from)
    }
}
