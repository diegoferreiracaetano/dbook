package com.dbook.infrastructure.persistence.crm

import com.dbook.AbstractIntegrationTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TheSearchMigrationMakesTheAccentInsensitiveIndexesUsableTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `given the migration when folding case and accents then it works, and the indexes exist`() {
        val folded = jdbcTemplate.queryForObject("SELECT immutable_unaccent(lower('JOSÉ'))", String::class.java)
        val indexes =
            jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename IN ('app_user', 'booking')",
                String::class.java,
            )

        assertEquals("jose", folded)
        assertTrue(
            indexes.containsAll(listOf("idx_app_user_name_trgm", "idx_app_user_email_trgm", "idx_booking_customer")),
        )
    }
}
