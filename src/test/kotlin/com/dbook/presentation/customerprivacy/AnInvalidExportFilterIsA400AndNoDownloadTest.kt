package com.dbook.presentation.customerprivacy

import kotlin.test.Test
import kotlin.test.assertEquals

class AnInvalidExportFilterIsA400AndNoDownloadTest : CustomerPrivacyFixture() {
    @Test
    fun `given an inverted period when exporting then it is 400`() {
        val result =
            exportCsv(
                superAdminToken(),
                "createdFrom" to "2026-02-01T00:00:00Z",
                "createdTo" to "2026-01-01T00:00:00Z",
            )

        assertEquals(400, result.response.status)
    }
}
