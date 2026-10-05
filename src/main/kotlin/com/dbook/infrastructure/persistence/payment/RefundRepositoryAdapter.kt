package com.dbook.infrastructure.persistence.payment

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.payment.Refund
import com.dbook.domain.payment.RefundAlreadyRequestedException
import com.dbook.domain.payment.RefundRepository
import com.dbook.domain.payment.RefundStatus
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
class RefundRepositoryAdapter(
    private val refundJpaRepository: RefundJpaRepository,
) : RefundRepository {
    // flushed right away so that the unique index answers here, where it can be turned into a 409, and not at commit
    override fun save(refund: Refund): Refund =
        try {
            refundJpaRepository.saveAndFlush(refund.toJpaEntity()).toDomain()
        } catch (ex: DataIntegrityViolationException) {
            throw RefundAlreadyRequestedException(ex)
        }

    override fun findById(id: Long): Refund? = refundJpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findInProgressByBookingId(bookingId: Long): Refund? =
        refundJpaRepository.findByBookingIdAndStatus(bookingId, RefundStatus.REQUESTED)?.toDomain()

    override fun findByRequestedByAndIdempotencyKey(
        requestedBy: Long,
        idempotencyKey: String,
    ): Refund? = refundJpaRepository.findByRequestedByAndIdempotencyKey(requestedBy, idempotencyKey)?.toDomain()

    override fun search(
        status: RefundStatus?,
        page: PageQuery,
    ): PageResult<Refund> {
        val request = PageRequest.of(page.page, page.size, Sort.by(Sort.Order.desc("id")))
        val found: Page<RefundJpaEntity> =
            if (status == null) {
                refundJpaRepository.findAll(
                    request,
                )
            } else {
                refundJpaRepository.findByStatus(status, request)
            }
        return PageResult(found.content.map { it.toDomain() }, page, found.totalElements)
    }
}
