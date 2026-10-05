package com.dbook.application.crm.anonymizecustomerusecase

import com.dbook.application.crm.AnonymizeCustomerCommand
import com.dbook.application.crm.CrmUseCaseFixture
import com.dbook.domain.identity.UserNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AStaffIdIsNotFoundAndAnAnonymizedOneCannotBeRepeatedTest : CrmUseCaseFixture() {
    @Test
    fun `given a staff id or an already anonymized customer when anonymizing then 404 and a conflict`() {
        assertFailsWith<UserNotFoundException> {
            anonymizeCustomer.execute(
                AnonymizeCustomerCommand(superAdmin, 4, "a reason that is long enough", "ANONYMIZE 4"),
            )
        }

        anonymizeCustomer.execute(
            AnonymizeCustomerCommand(superAdmin, 3, "a reason that is long enough", "ANONYMIZE 3"),
        )

        assertFailsWith<IllegalStateException> {
            anonymizeCustomer.execute(
                AnonymizeCustomerCommand(superAdmin, 3, "a reason that is long enough", "ANONYMIZE 3"),
            )
        }
    }
}
