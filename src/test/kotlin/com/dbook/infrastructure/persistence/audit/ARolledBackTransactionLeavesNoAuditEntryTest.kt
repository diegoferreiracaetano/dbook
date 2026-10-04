package com.dbook.infrastructure.persistence.audit

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// The point of auditing inside the use case's transaction: the change and its trail succeed or fail together.
class ARolledBackTransactionLeavesNoAuditEntryTest : AuditLogRepositoryAdapterFixture() {
    @Autowired
    lateinit var transactionManager: PlatformTransactionManager

    @Test
    fun `given an audit entry recorded inside a transaction when it rolls back then no entry is left`() {
        val actorId = uniqueActorId()

        assertFailsWith<IllegalStateException> {
            TransactionTemplate(transactionManager).executeWithoutResult {
                auditLog.record(event(actorId))
                error("the change failed after the trail was written")
            }
        }

        assertTrue(entriesOf(actorId).isEmpty())
    }
}
