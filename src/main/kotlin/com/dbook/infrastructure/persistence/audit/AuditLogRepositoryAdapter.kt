package com.dbook.infrastructure.persistence.audit

import com.dbook.domain.audit.AuditCursor
import com.dbook.domain.audit.AuditEvent
import com.dbook.domain.audit.AuditFilter
import com.dbook.domain.audit.AuditLog
import com.dbook.domain.audit.AuditLogReader
import com.dbook.domain.audit.AuditPage
import com.dbook.infrastructure.web.RequestAuditContext
import io.micrometer.core.instrument.MeterRegistry
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Repository
class AuditLogRepositoryAdapter(
    private val auditLogJpaRepository: AuditLogJpaRepository,
    private val requestAuditContext: RequestAuditContext,
    private val meterRegistry: MeterRegistry,
    private val clock: Clock,
) : AuditLog, AuditLogReader {
    private val log = LoggerFactory.getLogger(javaClass)

    // joins the transaction of the use case that calls it: the change and its trail commit or roll back together
    @Transactional
    override fun record(event: AuditEvent) {
        auditLogJpaRepository.save(event.toJpaEntity(clock.instant(), requestAuditContext.current()))
        meterRegistry.counter("dbook.admin.action", "action", event.action.name, "outcome", event.outcome.name)
            .increment()
        log.info(
            "audit {} {} on {}:{}",
            event.action,
            event.outcome,
            event.targetType,
            event.targetId,
            kv("audit_action", event.action.name),
            kv("audit_outcome", event.outcome.name),
            kv("actor_id", event.actor.id),
        )
    }

    override fun search(
        filter: AuditFilter,
        after: AuditCursor?,
        limit: Int,
    ): AuditPage {
        // one more than asked, only to know whether there is a next page
        val rows =
            auditLogJpaRepository.findBy<AuditLogJpaEntity, List<AuditLogJpaEntity>>(filter.toSpecification(after)) {
                it.sortBy(NEWEST_FIRST).limit(limit + 1).all()
            }
        val page = rows.take(limit).map { it.toDomain() }
        val next = if (rows.size > limit) page.last().let { AuditCursor(it.occurredAt, it.id) } else null
        return AuditPage(page, next)
    }

    private companion object {
        val NEWEST_FIRST: Sort = Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))
    }
}
