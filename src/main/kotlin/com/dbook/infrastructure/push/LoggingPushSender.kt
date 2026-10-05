package com.dbook.infrastructure.push

import com.dbook.domain.notification.DeviceToken
import com.dbook.domain.notification.PushSender
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Stands in for a real push service: it only records that a push would have gone out. It logs how many devices and what
 * kind, never the title, the body or the tokens (a token is a credential to push to that device).
 *
 * The real adapter (Firebase Cloud Messaging) replaces this class and changes nothing else: it needs a Firebase project
 * and a service account, which the project does not have, so it stays a registered evolution (docs/notificacoes.md).
 */
@Component
class LoggingPushSender : PushSender {
    override fun send(
        devices: List<DeviceToken>,
        title: String,
        body: String,
        data: Map<String, Any?>,
    ) {
        log.info("push to {} device(s): {}", devices.size, devices.groupingBy { it.platform }.eachCount())
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(LoggingPushSender::class.java)
    }
}
