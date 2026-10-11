package com.dbook.application.crm

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.common.audit.AuditEvent
import com.dbook.domain.common.audit.AuditLog
import com.dbook.domain.crm.CustomerBooking
import com.dbook.domain.crm.CustomerHistory
import com.dbook.domain.crm.CustomerPayment
import com.dbook.domain.crm.CustomerReview
import com.dbook.domain.favorite.FavoriteReader
import com.dbook.domain.favorite.FavoriteView
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserNotFoundException
import com.dbook.domain.identity.UserProfile
import com.dbook.domain.identity.UserProfileRepository
import com.dbook.domain.identity.UserRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

data class MyDataExport(
    val user: User,
    val profile: UserProfile,
    val bookings: List<CustomerBooking>,
    val payments: List<CustomerPayment>,
    val reviews: List<CustomerReview>,
    val favorites: List<FavoriteView>,
)

// Data portability: everything the system holds about the caller, in a machine-readable form.
@Observed(name = "dbook.usecase")
@Service
class ExportMyDataUseCase(
    private val userRepository: UserRepository,
    private val userProfiles: UserProfileRepository,
    private val customerHistory: CustomerHistory,
    private val favoriteReader: FavoriteReader,
    private val auditLog: AuditLog,
) {
    fun execute(userId: Long): MyDataExport {
        val user = userRepository.findById(userId) ?: throw UserNotFoundException(userId)
        val export =
            MyDataExport(
                user = user,
                profile = userProfiles.find(userId) ?: UserProfile(userId),
                bookings = allPages { customerHistory.bookings(userId, it) },
                payments = allPages { customerHistory.payments(userId, it) },
                reviews = allPages { customerHistory.reviews(userId, it) },
                favorites = allPages { favoriteReader.list(userId, null, it) },
            )
        auditLog.record(AuditEvent(Actor(userId, user.role), AuditAction.CUSTOMER_DATA_EXPORTED, userId.toString()))
        return export
    }

    // every page, with no cap: cutting a portability export short would be a silent omission
    private fun <T> allPages(read: (PageQuery) -> PageResult<T>): List<T> {
        val all = mutableListOf<T>()
        var page = 0
        while (true) {
            val result = read(PageQuery(page++, PageQuery.MAX_SIZE))
            all += result.items
            if (result.items.isEmpty() || all.size >= result.totalElements) {
                return all
            }
        }
    }
}
