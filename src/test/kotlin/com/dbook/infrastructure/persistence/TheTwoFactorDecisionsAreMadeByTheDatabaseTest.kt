package com.dbook.infrastructure.persistence

import com.dbook.AbstractIntegrationTest
import com.dbook.domain.identity.TwoFactorRepository
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TheTwoFactorDecisionsAreMadeByTheDatabaseTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var repository: TwoFactorRepository

    @Autowired
    lateinit var users: UserRepository

    private fun staffMember(): Long =
        requireNotNull(
            users.save(User(email = "tf${(1..999_999_999).random()}@example.com", passwordHash = "x", name = "T")).id,
        )

    private fun <T> race(
        times: Int,
        action: () -> T,
    ): List<T> {
        val pool = Executors.newFixedThreadPool(times)
        val start = CountDownLatch(1)
        try {
            val futures =
                List(times) {
                    pool.submit<T> {
                        start.await()
                        action()
                    }
                }
            start.countDown()
            return futures.map { it.get(30, TimeUnit.SECONDS) }
        } finally {
            pool.shutdown()
        }
    }

    @Test
    fun `given a confirmed enrollment when twenty requests use the same step then exactly one wins`() {
        val id = staffMember()
        repository.savePending(id, "v1:secret")
        repository.confirm(id, step = 100, now = Instant.now(), recoveryCodeHashes = emptyList())

        val wins = race(20) { repository.advanceStep(id, 101) }

        assertEquals(1, wins.count { it })
        assertFalse(repository.advanceStep(id, 101), "the same step is spent")
        assertFalse(repository.advanceStep(id, 100), "an older step never comes back")
        assertTrue(repository.advanceStep(id, 102))
    }

    @Test
    fun `given an unspent recovery code when twenty requests use it then exactly one wins`() {
        val id = staffMember()
        repository.savePending(id, "v1:secret")
        repository.confirm(id, step = 1, now = Instant.now(), recoveryCodeHashes = listOf("h1", "h2"))

        val wins = race(20) { repository.spendRecoveryCode(id, "h1", Instant.now()) }

        assertEquals(1, wins.count { it })
        assertEquals(1, repository.unspentRecoveryCodes(id))
    }

    @Test
    fun `given a pending enrollment when it starts again then the secret changes, a confirmed one never`() {
        val id = staffMember()
        repository.savePending(id, "v1:first")
        repository.savePending(id, "v1:second")
        assertEquals("v1:second", repository.find(id)?.secretEncrypted)

        repository.confirm(id, step = 5, now = Instant.now(), recoveryCodeHashes = listOf("h"))
        repository.savePending(id, "v1:third")

        assertEquals("v1:second", repository.find(id)?.secretEncrypted)
        assertTrue(repository.find(id)?.isActive == true)
    }

    @Test
    fun `given a pending enrollment when a step advances then nothing, and it confirms only once`() {
        val id = staffMember()
        repository.savePending(id, "v1:secret")

        assertFalse(repository.advanceStep(id, 7))
        assertTrue(repository.confirm(id, 1, Instant.now(), emptyList()))
        assertFalse(repository.confirm(id, 2, Instant.now(), emptyList()), "confirming twice is refused")
    }

    @Test
    fun `given a second factor when it is removed then it and its recovery codes are gone`() {
        val id = staffMember()
        repository.savePending(id, "v1:secret")
        repository.confirm(id, 1, Instant.now(), listOf("a", "b", "c"))

        repository.remove(id)

        assertEquals(null, repository.find(id))
        assertEquals(0, repository.unspentRecoveryCodes(id))
    }
}
