package com.dbook.infrastructure.persistence.crm

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.crm.CustomerNote
import com.dbook.domain.crm.CustomerNoteRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
class CustomerNoteRepositoryAdapter(
    private val customerNoteJpaRepository: CustomerNoteJpaRepository,
) : CustomerNoteRepository {
    override fun save(note: CustomerNote): CustomerNote = customerNoteJpaRepository.save(note.toJpaEntity()).toDomain()

    override fun findActive(
        customerId: Long,
        noteId: Long,
    ): CustomerNote? = customerNoteJpaRepository.findByIdAndCustomerIdAndDeletedAtIsNull(noteId, customerId)?.toDomain()

    override fun findActiveByCustomer(
        customerId: Long,
        page: PageQuery,
    ): PageResult<CustomerNote> {
        val order = Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        val found =
            customerNoteJpaRepository.findByCustomerIdAndDeletedAtIsNull(
                customerId,
                PageRequest.of(page.page, page.size, order),
            )
        return PageResult(found.content.map { it.toDomain() }, page, found.totalElements)
    }
}
