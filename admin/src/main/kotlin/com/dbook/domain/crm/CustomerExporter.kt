package com.dbook.domain.crm

// Streams the customers one by one, up to [limit], without ever holding them all in memory. Returns how many it sent.
interface CustomerExporter {
    fun export(
        filter: CustomerFilter,
        sort: CustomerSort,
        limit: Int,
        consumer: (CustomerSummary) -> Unit,
    ): Int
}
