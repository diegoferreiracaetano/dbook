package com.dbook.application.payment

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.payment.Refund
import com.dbook.domain.payment.RefundNotFoundException
import com.dbook.domain.payment.RefundRepository
import com.dbook.domain.payment.RefundStatus
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

@Observed(name = "dbook.usecase")
@Service
class ListRefundsUseCase(
    private val refundRepository: RefundRepository,
) {
    fun execute(
        status: RefundStatus?,
        page: PageQuery,
    ): PageResult<Refund> = refundRepository.search(status, page)

    fun find(id: Long): Refund = refundRepository.findById(id) ?: throw RefundNotFoundException(id)
}
