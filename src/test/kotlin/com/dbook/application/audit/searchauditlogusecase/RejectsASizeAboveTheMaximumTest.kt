package com.dbook.application.audit.searchauditlogusecase

import com.dbook.application.audit.SearchAuditLogQuery
import com.dbook.application.audit.SearchAuditLogUseCase
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class RejectsASizeAboveTheMaximumTest : SearchAuditLogUseCaseFixture() {
    @Test
    fun `given a page size over the maximum when searching then it throws and the reader is not asked`() {
        assertFailsWith<IllegalArgumentException> {
            useCase.execute(SearchAuditLogQuery(size = SearchAuditLogUseCase.MAX_SIZE + 1))
        }

        assertNull(reader.lastLimit)
    }
}
