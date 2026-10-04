package com.dbook.infrastructure.persistence.audit

import com.dbook.domain.audit.AuditCursor
import com.dbook.domain.audit.AuditFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchPagesNewestFirstWithAKeysetCursorTest : AuditLogRepositoryAdapterFixture() {
    @Test
    fun `given five entries when paged by two then pages are 2, 2 and 1, newest first, with no gap or repeat`() {
        val actorId = uniqueActorId()
        repeat(5) { index -> auditLog.record(event(actorId, targetId = "target-$index")) }
        val filter = AuditFilter(actorId = actorId)

        val pages = mutableListOf<List<Long>>()
        var cursor: AuditCursor? = null
        do {
            val page = auditLogReader.search(filter, cursor, limit = 2)
            pages += page.entries.map { it.id }
            cursor = page.next
        } while (cursor != null)

        assertEquals(listOf(2, 2, 1), pages.map { it.size })
        val allIds = pages.flatten()
        assertEquals(allIds.sortedDescending(), allIds)
        assertEquals(5, allIds.toSet().size)
        assertNull(auditLogReader.search(filter, null, limit = 5).next)
    }
}
