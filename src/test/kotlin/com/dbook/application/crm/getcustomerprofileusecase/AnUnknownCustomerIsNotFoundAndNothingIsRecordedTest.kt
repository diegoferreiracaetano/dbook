package com.dbook.application.crm.getcustomerprofileusecase

import com.dbook.application.crm.CrmUseCaseFixture
import com.dbook.domain.identity.UserNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AnUnknownCustomerIsNotFoundAndNothingIsRecordedTest : CrmUseCaseFixture() {
    @Test
    fun `given an unknown customer when opening the profile then it is not found and no view is recorded`() {
        assertFailsWith<UserNotFoundException> { getProfile.execute(support, 99) }

        assertTrue(audit.events.isEmpty())
    }
}
