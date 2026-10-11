package com.dbook.application.crm.exportmydatausecase

import com.dbook.application.audit.FakeAuditLog
import com.dbook.application.crm.ExportMyDataUseCase
import com.dbook.application.identity.FakeUserProfileRepository
import com.dbook.application.identity.staff.InMemoryUserRepository
import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.crm.CustomerBooking
import com.dbook.domain.crm.CustomerHistory
import com.dbook.domain.crm.CustomerPayment
import com.dbook.domain.crm.CustomerReview
import com.dbook.domain.favorite.FavoriteReader
import com.dbook.domain.favorite.FavoriteType
import com.dbook.domain.favorite.FavoriteView
import com.dbook.domain.identity.User
import java.math.BigDecimal

// A customer with [bookingCount] bookings, served page by page the way the real adapter does.
class PagedHistory(bookingCount: Int) : CustomerHistory {
    private val bookings =
        List(bookingCount) {
            CustomerBooking(it + 1L, "CONFIRMED", BigDecimal("100.00"), "Flight", "1A", "DB1", "GRU", "GIG", null, null)
        }
    var bookingPagesRead = 0

    override fun customerExists(id: Long) = true

    override fun bookings(
        customerId: Long,
        page: PageQuery,
    ): PageResult<CustomerBooking> {
        bookingPagesRead++
        return PageResult(bookings.drop(page.offset.toInt()).take(page.size), page, bookings.size.toLong())
    }

    override fun payments(
        customerId: Long,
        page: PageQuery,
    ) = PageResult<CustomerPayment>(emptyList(), page, 0)

    override fun reviews(
        customerId: Long,
        page: PageQuery,
    ) = PageResult<CustomerReview>(emptyList(), page, 0)
}

private object NoFavorites : FavoriteReader {
    override fun list(
        userId: Long,
        type: FavoriteType?,
        page: PageQuery,
    ) = PageResult<FavoriteView>(emptyList(), page, 0)
}

abstract class ExportMyDataUseCaseFixture {
    protected val audit = FakeAuditLog()
    protected val history = PagedHistory(bookingCount = 250)
    protected val profiles = FakeUserProfileRepository()
    protected val useCase =
        ExportMyDataUseCase(
            InMemoryUserRepository(User(id = 3, email = "customer@example.com", passwordHash = "x", name = "Customer")),
            profiles,
            history,
            NoFavorites,
            audit,
        )
}
