package com.dbook.infrastructure.persistence.crm

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface CustomerNoteJpaRepository : JpaRepository<CustomerNoteJpaEntity, Long> {
    fun findByIdAndCustomerIdAndDeletedAtIsNull(
        id: Long,
        customerId: Long,
    ): CustomerNoteJpaEntity?

    fun findByCustomerIdAndDeletedAtIsNull(
        customerId: Long,
        pageable: Pageable,
    ): Page<CustomerNoteJpaEntity>
}
