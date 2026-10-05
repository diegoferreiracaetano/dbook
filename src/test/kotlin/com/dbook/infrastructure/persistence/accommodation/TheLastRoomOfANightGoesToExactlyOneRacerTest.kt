package com.dbook.infrastructure.persistence.accommodation

import com.dbook.AbstractIntegrationTest
import com.dbook.domain.accommodation.RoomInventory
import com.dbook.domain.accommodation.RoomUnavailableException
import com.dbook.domain.booking.Stay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TheLastRoomOfANightGoesToExactlyOneRacerTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var inventory: RoomInventory

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var transactions: PlatformTransactionManager

    private val first = LocalDate.now().plusDays(200)

    private fun aRoomType(quantity: Int): Long {
        val airport = jdbcTemplate.queryForObject("SELECT id FROM airport ORDER BY id LIMIT 1", Long::class.java)
        val bookable =
            jdbcTemplate.queryForObject(
                "INSERT INTO bookable (title, price, total_capacity, active, version) " +
                    "VALUES ('Inventory hotel', 100, ?, true, 0) RETURNING id",
                Long::class.java,
                quantity,
            )
        jdbcTemplate.update(
            "INSERT INTO accommodation (id, destination_airport_id, address, stars) VALUES (?, ?, 'Rua 1', 3)",
            bookable,
            airport,
        )
        return requireNotNull(
            jdbcTemplate.queryForObject(
                "INSERT INTO room_type (accommodation_id, name, capacity, nightly_rate, quantity) " +
                    "VALUES (?, 'Double', 2, 100, ?) RETURNING id",
                Long::class.java,
                bookable,
                quantity,
            ),
        )
    }

    private fun stay(
        room: Long,
        from: Int,
        to: Int,
    ) = Stay(room, first.plusDays(from.toLong()), first.plusDays(to.toLong()), 2, BigDecimal("100.00"))

    private fun taken(room: Long): Map<LocalDate, Int> =
        jdbcTemplate.query(
            "SELECT night, booked FROM room_night WHERE room_type_id = ?",
            { rs, _ -> rs.getDate("night").toLocalDate() to rs.getInt("booked") },
            room,
        ).toMap()

    private fun reserveInTransaction(stay: Stay) = TransactionTemplate(transactions).execute { inventory.reserve(stay) }

    @Test
    fun `given one room and 20 stays of the same nights at once then exactly one is taken and nothing is held twice`() {
        val room = aRoomType(1)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(RACERS)

        val results =
            (1..RACERS).map {
                pool.submit<Boolean> {
                    start.await()
                    runCatching { reserveInTransaction(stay(room, 0, 3)) }.isSuccess
                }
            }.also { start.countDown() }.map { it.get() }
        pool.shutdown()

        assertEquals(1, results.count { it })
        assertEquals(setOf(1), taken(room).values.toSet())
        assertEquals(3, taken(room).size)
    }

    @Test
    fun `given a quantity of three when ten stays race for one night then exactly three are taken`() {
        val room = aRoomType(3)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(TEN)

        val results =
            (1..TEN).map {
                pool.submit<Boolean> {
                    start.await()
                    runCatching { reserveInTransaction(stay(room, 0, 1)) }.isSuccess
                }
            }.also { start.countDown() }.map { it.get() }
        pool.shutdown()

        assertEquals(3, results.count { it })
        assertEquals(3, taken(room)[first])
    }

    @Test
    fun `given a stay whose last night is full when reserved then the nights before it are undone with it`() {
        val room = aRoomType(1)
        reserveInTransaction(stay(room, 3, 4))

        assertFailsWith<RoomUnavailableException> { reserveInTransaction(stay(room, 0, 4)) }

        assertEquals(mapOf(first.plusDays(3) to 1), taken(room))
    }

    @Test
    fun `given a released stay when released again or when asked about then the count never goes below zero`() {
        val room = aRoomType(1)
        val stay = stay(room, 0, 2)
        reserveInTransaction(stay)
        assertEquals(false, inventory.isAvailable(stay))

        TransactionTemplate(transactions).execute { inventory.release(stay) }
        TransactionTemplate(transactions).execute { inventory.release(stay) }

        assertTrue(taken(room).values.all { it == 0 })
        assertEquals(true, inventory.isAvailable(stay))
    }

    @Test
    fun `given booked nights when asked for the most booked from a date then it is the highest count from then on`() {
        val room = aRoomType(3)
        reserveInTransaction(stay(room, 0, 2))
        reserveInTransaction(stay(room, 1, 3))

        assertEquals(2, inventory.maxBookedFrom(room, first))
        assertEquals(1, inventory.maxBookedFrom(room, first.plusDays(2)))
        assertEquals(0, inventory.maxBookedFrom(room, first.plusDays(3)))
    }

    private companion object {
        const val RACERS = 20
        const val TEN = 10
    }
}
