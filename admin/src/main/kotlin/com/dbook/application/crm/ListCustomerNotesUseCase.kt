package com.dbook.application.crm

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.crm.CustomerNote
import com.dbook.domain.crm.CustomerNoteRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

@Observed(name = "dbook.usecase")
@Service
class ListCustomerNotesUseCase(
    private val customerGuard: CustomerGuard,
    private val notes: CustomerNoteRepository,
) {
    fun execute(
        customerId: Long,
        page: PageQuery,
    ): PageResult<CustomerNote> {
        customerGuard.customer(customerId)
        return notes.findActiveByCustomer(customerId, page)
    }
}
