package com.dbook.application.crm.exportmydatausecase

import com.dbook.domain.common.audit.AuditAction
import kotlin.test.Test
import kotlin.test.assertEquals

class ExportsEveryPageWithoutACapAndRecordsItTest : ExportMyDataUseCaseFixture() {
    @Test
    fun `given 250 bookings when exporting then all of them come, in three pages, and the export is recorded`() {
        val export = useCase.execute(3)

        assertEquals(250, export.bookings.size)
        assertEquals(3, history.bookingPagesRead)
        assertEquals(AuditAction.CUSTOMER_DATA_EXPORTED, audit.events.single().action)
        assertEquals(3L, audit.events.single().actor.id)
    }
}
