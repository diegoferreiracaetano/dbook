package com.dbook.infrastructure.persistence

import com.dbook.AbstractIntegrationTest
import com.dbook.domain.identity.AccountToken
import com.dbook.domain.identity.AccountTokenPurpose
import com.dbook.domain.identity.AccountTokenRepository
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TheAccountLinkIsSpentByTheDatabaseTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var tokens: AccountTokenRepository

    @Autowired
    lateinit var users: UserRepository

    private val now: Instant = Instant.now()

    private fun userId(): Long =
        requireNotNull(
            users.save(User(email = "link${(1..999_999_999).random()}@example.com", passwordHash = "x", name = "L")).id,
        )

    private fun link(
        userId: Long,
        purpose: AccountTokenPurpose = AccountTokenPurpose.PASSWORD_RESET,
        hash: String = "h${(1..999_999_999).random()}",
    ) = tokens.issue(AccountToken.issue(userId, purpose, hash, now))

    @Test
    fun `given one link when twenty requests spend it at once then exactly one wins`() {
        val id = requireNotNull(link(userId()).id)
        val pool = Executors.newFixedThreadPool(20)
        val start = CountDownLatch(1)
        try {
            val wins =
                List(20) {
                    pool.submit<Boolean> {
                        start.await()
                        tokens.consume(id, now.plusSeconds(1))
                    }
                }
                    .also { start.countDown() }
                    .map { it.get(30, TimeUnit.SECONDS) }

            assertEquals(1, wins.count { it })
        } finally {
            pool.shutdown()
        }
    }

    @Test
    fun `given a link past its expiry when it is spent then it is refused`() {
        val saved = link(userId())

        assertFalse(
            tokens.consume(
                requireNotNull(saved.id),
                now.plus(AccountTokenPurpose.PASSWORD_RESET.validity).plusSeconds(1),
            ),
        )
        assertTrue(tokens.consume(requireNotNull(saved.id), now.plus(Duration.ofMinutes(5))), "still inside the hour")
    }

    @Test
    fun `given a new link when issued then the older one of that purpose is closed and the other is not`() {
        val user = userId()
        val old = link(user, AccountTokenPurpose.PASSWORD_RESET, "old-hash-$user")
        val other = link(user, AccountTokenPurpose.EMAIL_VERIFICATION, "other-hash-$user")

        val fresh = link(user, AccountTokenPurpose.PASSWORD_RESET, "fresh-hash-$user")

        assertFalse(tokens.consume(requireNotNull(old.id), now.plusSeconds(1)))
        assertTrue(tokens.consume(requireNotNull(other.id), now.plusSeconds(1)))
        assertTrue(tokens.consume(requireNotNull(fresh.id), now.plusSeconds(1)))
    }

    @Test
    fun `given a stored link when found by its hash then it comes back whole and an unknown hash finds none`() {
        val user = userId()
        val saved = link(user, hash = "find-me-$user")

        val found = assertNotNull(tokens.findByTokenHash("find-me-$user"))

        assertEquals(saved.id, found.id)
        assertEquals(AccountTokenPurpose.PASSWORD_RESET, found.purpose)
        assertEquals(null, found.usedAt)
        assertEquals(null, tokens.findByTokenHash("nothing"))
    }
}
