package com.dbook.application.crm

import com.dbook.domain.audit.AuditCursor
import com.dbook.domain.audit.AuditEntry
import com.dbook.domain.audit.AuditFilter
import com.dbook.domain.audit.AuditLogReader
import com.dbook.domain.audit.AuditPage
import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.crm.BookingTotals
import com.dbook.domain.crm.CustomerErasure
import com.dbook.domain.crm.CustomerNote
import com.dbook.domain.crm.CustomerNoteRepository
import com.dbook.domain.crm.CustomerProfile
import com.dbook.domain.crm.CustomerProfileReader
import com.dbook.domain.crm.PaymentTotals
import com.dbook.domain.crm.ReviewTotals
import com.dbook.domain.identity.UserStatus
import java.math.BigDecimal
import java.time.Instant

class RecordingCustomerErasure : CustomerErasure {
    val erased = mutableListOf<Long>()

    override fun erasePersonalData(customerId: Long) {
        erased += customerId
    }
}

class InMemoryCustomerNotes : CustomerNoteRepository {
    private val notes = mutableListOf<CustomerNote>()
    private var lastId = 0L

    override fun save(note: CustomerNote): CustomerNote {
        val stored =
            if (note.id == null) {
                CustomerNote(++lastId, note.customerId, note.authorId, note.body, note.pinned, note.createdAt)
            } else {
                note
            }
        notes.removeAll { it.id == stored.id }
        notes += stored
        return stored
    }

    fun byId(id: Long): CustomerNote? = notes.find { it.id == id }

    override fun findActive(
        customerId: Long,
        noteId: Long,
    ): CustomerNote? = notes.find { it.id == noteId && it.customerId == customerId && !it.isDeleted }

    override fun findActiveByCustomer(
        customerId: Long,
        page: PageQuery,
    ): PageResult<CustomerNote> {
        val all = notes.filter { it.customerId == customerId && !it.isDeleted }
        return PageResult(all, page, all.size.toLong())
    }
}

/** Answers the audit search with whatever entries it was given, and keeps the filter it was asked with. */
class StubAuditLogReader(var entries: List<AuditEntry> = emptyList()) : AuditLogReader {
    var lastFilter: AuditFilter? = null

    override fun search(
        filter: AuditFilter,
        after: AuditCursor?,
        limit: Int,
    ): AuditPage {
        lastFilter = filter
        return AuditPage(entries, null)
    }
}

class FixedCustomerProfileReader(private val profile: CustomerProfile?) : CustomerProfileReader {
    override fun find(id: Long): CustomerProfile? = profile?.takeIf { it.id == id }
}

fun aProfile(id: Long) =
    CustomerProfile(
        id = id,
        name = "Customer",
        email = "customer@example.com",
        status = UserStatus.ACTIVE,
        blockedReason = null,
        blockedAt = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        lastLoginAt = null,
        anonymizedAt = null,
        bookings = BookingTotals(0, 0, 0),
        payments = PaymentTotals(0, BigDecimal.ZERO),
        reviews = ReviewTotals(0, null),
    )
