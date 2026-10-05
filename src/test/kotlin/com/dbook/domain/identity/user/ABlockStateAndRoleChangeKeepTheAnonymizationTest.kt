package com.dbook.domain.identity.user

import com.dbook.domain.identity.User
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertTrue

class ABlockStateAndRoleChangeKeepTheAnonymizationTest {
    @Test
    fun `given an anonymized account when renamed or given a new hash then it is still anonymized`() {
        val customer = User(id = 7, email = "m@example.com", passwordHash = "h", name = "M")
        val anonymized = customer.anonymize(Instant.now())

        assertTrue(anonymized.rename("Other").isAnonymized)
        assertTrue(anonymized.withPasswordHash("new").isAnonymized)
    }
}
