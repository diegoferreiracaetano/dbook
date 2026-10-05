package com.dbook.application.crm

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.crm.CustomerBooking
import com.dbook.domain.crm.CustomerHistory
import com.dbook.domain.crm.CustomerPayment
import com.dbook.domain.crm.CustomerReview
import com.dbook.domain.identity.UserNotFoundException
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

// One class for the three lists: they share the same guard (the id must be a customer, otherwise 404, like the
// profile) and differ only in what they read.
@Observed(name = "dbook.usecase")
@Service
class ListCustomerHistoryUseCase(
    private val customerHistory: CustomerHistory,
) {
    fun bookings(
        customerId: Long,
        page: PageQuery,
    ): PageResult<CustomerBooking> = ofCustomer(customerId) { customerHistory.bookings(customerId, page) }

    fun payments(
        customerId: Long,
        page: PageQuery,
    ): PageResult<CustomerPayment> = ofCustomer(customerId) { customerHistory.payments(customerId, page) }

    fun reviews(
        customerId: Long,
        page: PageQuery,
    ): PageResult<CustomerReview> = ofCustomer(customerId) { customerHistory.reviews(customerId, page) }

    private fun <T> ofCustomer(
        customerId: Long,
        read: () -> T,
    ): T {
        if (!customerHistory.customerExists(customerId)) {
            throw UserNotFoundException(customerId)
        }
        return read()
    }
}
