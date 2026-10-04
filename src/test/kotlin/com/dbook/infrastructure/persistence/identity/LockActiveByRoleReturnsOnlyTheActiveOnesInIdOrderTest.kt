package com.dbook.infrastructure.persistence.identity

import com.dbook.domain.identity.Role
import com.dbook.domain.identity.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LockActiveByRoleReturnsOnlyTheActiveOnesInIdOrderTest : UserRepositoryAdapterFixture() {
    @Autowired
    lateinit var transactionManager: PlatformTransactionManager

    @Test
    fun `given an active and a blocked super admin when locking in a transaction then only the active one returns`() {
        val active = superAdmin()
        val blocked = superAdmin().block("spam", Instant.now())
        val blockedId = requireNotNull(userRepository.save(blocked).id)

        val locked =
            TransactionTemplate(
                transactionManager,
            ).execute { userRepository.lockActiveByRole(Role.SUPER_ADMIN) }
        val ids = requireNotNull(locked).map { requireNotNull(it.id) }

        assertTrue(requireNotNull(active.id) in ids)
        assertFalse(blockedId in ids)
        assertEquals(ids.sorted(), ids)
    }

    private fun superAdmin(): User =
        userRepository.save(
            User(
                email = "root${(1..999_999_999).random()}@example.com",
                passwordHash = "h",
                name = "Root",
                role = Role.SUPER_ADMIN,
            ),
        )
}
