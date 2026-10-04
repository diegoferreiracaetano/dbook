package com.dbook.application.audit.searchauditlogusecase

import com.dbook.application.audit.SearchAuditLogQuery
import com.dbook.domain.audit.AuditAction
import com.dbook.domain.audit.AuditCursor
import com.dbook.domain.audit.AuditFilter
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class PassesTheFilterTheCursorAndTheSizeToTheReaderTest : SearchAuditLogUseCaseFixture() {
    @Test
    fun `given a query when searching then the reader gets exactly its filter, cursor and size`() {
        val filter = AuditFilter(actorId = 3, action = AuditAction.FLIGHT_CREATED)
        val cursor = AuditCursor(Instant.parse("2026-10-04T12:00:00Z"), 42)

        useCase.execute(SearchAuditLogQuery(filter, cursor, size = 20))

        assertEquals(filter, reader.lastFilter)
        assertEquals(cursor, reader.lastAfter)
        assertEquals(20, reader.lastLimit)
    }
}
