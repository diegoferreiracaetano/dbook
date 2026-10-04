package com.dbook.infrastructure.persistence.audit

import com.dbook.domain.audit.AuditAction
import com.dbook.domain.audit.AuditOutcome
import com.dbook.domain.identity.Role
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

// Immutable: Hibernate never checks it for changes (the JSON columns would always look changed after the insert,
// and an UPDATE is exactly what the table's trigger refuses).
@Entity
@Immutable
@Table(name = "audit_log")
class AuditLogJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var occurredAt: Instant = Instant.EPOCH,
    var actorId: Long = 0,
    @Enumerated(EnumType.STRING)
    var actorRole: Role = Role.CLIENT,
    @Enumerated(EnumType.STRING)
    var action: AuditAction = AuditAction.ACCESS_DENIED,
    @Enumerated(EnumType.STRING)
    var outcome: AuditOutcome = AuditOutcome.SUCCESS,
    var targetType: String = "",
    var targetId: String = "",
    @JdbcTypeCode(SqlTypes.JSON)
    var stateBefore: MutableMap<String, Any?>? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    var stateAfter: MutableMap<String, Any?>? = null,
    var reason: String? = null,
    @Embedded
    var context: AuditContextColumns = AuditContextColumns(),
)
