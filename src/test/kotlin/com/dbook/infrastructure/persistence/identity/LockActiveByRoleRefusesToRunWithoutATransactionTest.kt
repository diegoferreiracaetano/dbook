package com.dbook.infrastructure.persistence.identity

import com.dbook.domain.identity.Role
import org.springframework.transaction.IllegalTransactionStateException
import kotlin.test.Test
import kotlin.test.assertFailsWith

// A lock taken in a transaction of its own would be released before the caller could rely on it.
class LockActiveByRoleRefusesToRunWithoutATransactionTest : UserRepositoryAdapterFixture() {
    @Test
    fun `given no surrounding transaction when locking the super admins then it fails`() {
        assertFailsWith<IllegalTransactionStateException> { userRepository.lockActiveByRole(Role.SUPER_ADMIN) }
    }
}
