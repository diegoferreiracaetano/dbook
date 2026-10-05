package com.dbook.infrastructure.persistence.notification

import com.dbook.AbstractIntegrationTest
import com.dbook.domain.notification.NotificationDeliveryLog
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

abstract class NotificationDeliveryLogFixture : AbstractIntegrationTest() {
    @Autowired
    lateinit var deliveryLog: NotificationDeliveryLog

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    protected fun statusOf(
        eventId: java.util.UUID,
        channel: String,
    ): String =
        requireNotNull(
            jdbcTemplate.queryForObject(
                "SELECT status FROM notification_delivery WHERE event_id = ?::uuid AND channel = ?",
                String::class.java,
                eventId.toString(),
                channel,
            ),
        )

    protected fun attemptsOf(
        eventId: java.util.UUID,
        channel: String,
    ): Int =
        requireNotNull(
            jdbcTemplate.queryForObject(
                "SELECT attempts FROM notification_delivery WHERE event_id = ?::uuid AND channel = ?",
                Int::class.java,
                eventId.toString(),
                channel,
            ),
        )
}
