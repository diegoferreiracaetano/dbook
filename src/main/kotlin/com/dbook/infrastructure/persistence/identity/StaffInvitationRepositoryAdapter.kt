package com.dbook.infrastructure.persistence.identity

import com.dbook.domain.identity.DuplicateOpenInvitationException
import com.dbook.domain.identity.StaffInvitation
import com.dbook.domain.identity.StaffInvitationRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class StaffInvitationRepositoryAdapter(
    private val staffInvitationJpaRepository: StaffInvitationJpaRepository,
) : StaffInvitationRepository {
    // flushed right away: re-inviting revokes the open invitation and creates another in the same transaction,
    // and the partial unique index only accepts that if the revocation reaches the database first
    override fun save(invitation: StaffInvitation): StaffInvitation =
        try {
            staffInvitationJpaRepository.saveAndFlush(invitation.toJpaEntity()).toDomain()
        } catch (ex: DataIntegrityViolationException) {
            throw DuplicateOpenInvitationException(ex)
        }

    override fun findById(id: Long): StaffInvitation? =
        staffInvitationJpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByTokenHash(tokenHash: String): StaffInvitation? =
        staffInvitationJpaRepository.findByTokenHash(tokenHash)?.toDomain()

    override fun findOpenByEmail(email: String): StaffInvitation? =
        staffInvitationJpaRepository.findFirstByEmailAndAcceptedAtIsNullAndRevokedAtIsNull(email)?.toDomain()

    override fun findRecent(): List<StaffInvitation> =
        staffInvitationJpaRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, MAX_LISTED)).map { it.toDomain() }

    private companion object {
        const val MAX_LISTED = 100
    }
}
