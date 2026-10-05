package com.dbook.infrastructure.persistence.favorite

import com.dbook.AbstractIntegrationTest
import com.dbook.domain.favorite.Favorite
import com.dbook.domain.favorite.FavoriteAddResult
import com.dbook.domain.favorite.FavoriteRepository
import com.dbook.domain.favorite.FavoriteType
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals

class TwoRequestsAtOnceNeverGoPastTheLimitTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var favorites: FavoriteRepository

    @Autowired
    lateinit var users: UserRepository

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `given a limit of 5 when 20 requests of the same customer race then exactly 5 are saved`() {
        val user =
            requireNotNull(users.save(User(null, "race${(1..999_999_999).random()}@example.com", "x", "Race")).id)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(THREADS)

        val results =
            (1..THREADS).map { n ->
                pool.submit<FavoriteAddResult> {
                    start.await()
                    favorites.add(Favorite(user, FavoriteType.FLIGHT, "$n", Instant.now()), LIMIT)
                }
            }.also { start.countDown() }.map { it.get() }
        pool.shutdown()

        assertEquals(LIMIT, results.count { it == FavoriteAddResult.ADDED })
        assertEquals(THREADS - LIMIT, results.count { it == FavoriteAddResult.LIMIT_REACHED })
        assertEquals(
            LIMIT,
            jdbcTemplate.queryForObject("SELECT count(*) FROM favorite WHERE user_id = ?", Int::class.java, user),
        )
    }

    private companion object {
        const val THREADS = 20
        const val LIMIT = 5
    }
}
