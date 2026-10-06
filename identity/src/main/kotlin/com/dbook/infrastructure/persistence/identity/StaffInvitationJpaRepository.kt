package com.dbook.infrastructure.persistence.identity

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface StaffInvitationJpaRepository : JpaRepository<StaffInvitationJpaEntity, Long> {
    fun findByTokenHash(tokenHash: String): StaffInvitationJpaEntity?

    fun findFirstByEmailAndAcceptedAtIsNullAndRevokedAtIsNull(email: String): StaffInvitationJpaEntity?

    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): List<StaffInvitationJpaEntity>
}
