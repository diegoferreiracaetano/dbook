package com.dbook.infrastructure.persistence.audit

import org.springframework.dao.DataAccessException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// The guarantee lives in the database (a trigger), not in the application code: it holds for anyone with access.
class TheTrailCannotBeUpdatedDeletedNorTruncatedTest : AuditLogRepositoryAdapterFixture() {
    @Test
    fun `given recorded entries when someone updates, deletes or truncates them then the database refuses`() {
        val actorId = uniqueActorId()
        auditLog.record(event(actorId))

        assertFailsWith<DataAccessException> {
            jdbcTemplate.update("UPDATE audit_log SET reason = 'tampered' WHERE actor_id = ?", actorId)
        }
        assertFailsWith<DataAccessException> {
            jdbcTemplate.update("DELETE FROM audit_log WHERE actor_id = ?", actorId)
        }
        assertFailsWith<DataAccessException> { jdbcTemplate.execute("TRUNCATE audit_log") }

        assertTrue(entriesOf(actorId).size == 1)
    }
}
