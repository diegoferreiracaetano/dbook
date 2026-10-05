package com.dbook.infrastructure.persistence.promo

import com.dbook.AbstractIntegrationTest
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserRepository
import com.dbook.domain.promo.PromoCode
import com.dbook.domain.promo.PromoRepository
import com.dbook.domain.promo.PromoType
import com.dbook.domain.promo.RedeemResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals

class TheLastUseGoesToExactlyOneOfManyRacersTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var promos: PromoRepository

    @Autowired
    lateinit var users: UserRepository

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var transactions: PlatformTransactionManager

    private fun aUser(): Long =
        requireNotNull(users.save(User(null, "racer${UUID.randomUUID()}@example.com", "x", "Racer")).id)

    private fun aPayment(user: Long): Long =
        requireNotNull(
            jdbcTemplate.queryForObject(
                "INSERT INTO payment (customer_id, amount, subtotal, card_last4, cardholder_name) " +
                    "VALUES (?, 90, 100, '4242', 'Racer') RETURNING id",
                Long::class.java,
                user,
            ),
        )

    private fun aPromo(
        maxRedemptions: Int?,
        maxPerUser: Int,
    ): Long {
        val now = Instant.now()
        val saved =
            promos.save(
                PromoCode(
                    code = "R" + UUID.randomUUID().toString().replace("-", "").take(CODE_LENGTH).uppercase(),
                    type = PromoType.FIXED, value = BigDecimal("5.00"), validFrom = now.minus(1, ChronoUnit.DAYS),
                    validUntil = now.plus(1, ChronoUnit.DAYS), maxRedemptions = maxRedemptions, maxPerUser = maxPerUser,
                    createdBy = aUser(), createdAt = now,
                ),
            )
        return requireNotNull(saved.id)
    }

    // each racer redeems inside its own transaction, as a payment does
    private fun race(
        promoId: Long,
        racers: List<Pair<Long, Long>>,
    ): List<RedeemResult> {
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(racers.size)
        val results =
            racers.map { (user, payment) ->
                pool.submit<RedeemResult> {
                    start.await()
                    TransactionTemplate(transactions).execute {
                        promos.redeem(promoId, user, payment, BigDecimal("5.00"), Instant.now())
                    }
                }
            }.also { start.countDown() }.map { it.get() }
        pool.shutdown()
        return results
    }

    @Test
    fun `given one use left when 20 customers redeem at once then exactly one is redeemed and the count is 1`() {
        val promo = aPromo(maxRedemptions = 1, maxPerUser = 1)
        val racers =
            List(RACERS) {
                val user = aUser()
                user to aPayment(user)
            }

        val results = race(promo, racers)

        assertEquals(1, results.count { it == RedeemResult.REDEEMED })
        assertEquals(RACERS - 1, results.count { it == RedeemResult.EXHAUSTED })
        assertEquals(1, promos.findById(promo)!!.redeemed)
    }

    @Test
    fun `given a once-per-customer code when one customer redeems 10 times at once then exactly one goes through`() {
        val promo = aPromo(maxRedemptions = null, maxPerUser = 1)
        val user = aUser()

        val results = race(promo, List(TEN) { user to aPayment(user) })

        assertEquals(1, results.count { it == RedeemResult.REDEEMED })
        assertEquals(TEN - 1, results.count { it == RedeemResult.USER_LIMIT_REACHED })
        assertEquals(1, promos.redemptionsBy(promo, user))
    }

    private companion object {
        const val CODE_LENGTH = 10
        const val RACERS = 20
        const val TEN = 10
    }
}
