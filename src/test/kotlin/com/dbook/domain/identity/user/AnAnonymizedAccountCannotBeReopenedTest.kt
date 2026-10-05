package com.dbook.domain.identity.user

import com.dbook.domain.identity.User
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AnAnonymizedAccountCannotBeReopenedTest {
    @Test
    fun `given an anonymized account when unblocking then it is refused`() {
        val customer = User(id = 7, email = "maria@example.com", passwordHash = "h", name = "Maria")
        val anonymized = customer.anonymize(Instant.now())

        assertFailsWith<IllegalStateException> { anonymized.unblock() }
    }
}
