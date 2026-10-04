package com.dbook.application.audit.searchauditlogusecase

import com.dbook.application.audit.SearchAuditLogQuery
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsASizeOfZeroTest : SearchAuditLogUseCaseFixture() {
    @Test
    fun `given a page size of zero when searching then it throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> { useCase.execute(SearchAuditLogQuery(size = 0)) }
    }
}
