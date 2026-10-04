package com.dbook.infrastructure.persistence.audit

import com.dbook.domain.audit.AuditCursor
import com.dbook.domain.audit.AuditFilter
import org.springframework.data.jpa.domain.Specification
import java.time.Instant

// Only the filters that were given become predicates. The cursor is a keyset: strictly older than
// (occurredAt, id) in the same order the page is sorted, so entries arriving meanwhile never shift a page.
fun AuditFilter.toSpecification(after: AuditCursor?): Specification<AuditLogJpaEntity> =
    Specification { root, _, cb ->
        val occurredAt = root.get<Instant>("occurredAt")
        val id = root.get<Long>("id")
        listOfNotNull(
            actorId?.let { cb.equal(root.get<Long>("actorId"), it) },
            action?.let { cb.equal(root.get<Any>("action"), it) },
            targetType?.let { cb.equal(root.get<String>("targetType"), it) },
            targetId?.let { cb.equal(root.get<String>("targetId"), it) },
            outcome?.let { cb.equal(root.get<Any>("outcome"), it) },
            from?.let { cb.greaterThanOrEqualTo(occurredAt, it) },
            to?.let { cb.lessThan(occurredAt, it) },
            after?.let {
                cb.or(
                    cb.lessThan(occurredAt, it.occurredAt),
                    cb.and(cb.equal(occurredAt, it.occurredAt), cb.lessThan(id, it.id)),
                )
            },
        ).fold(cb.conjunction()) { all, predicate -> cb.and(all, predicate) }
    }
