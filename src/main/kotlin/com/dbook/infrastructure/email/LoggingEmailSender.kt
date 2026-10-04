package com.dbook.infrastructure.email

import com.dbook.domain.identity.EmailSender
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.stereotype.Component

@Component
class LoggingEmailSender(
    @Value("\${email.log-body}") private val logBody: Boolean,
    environment: Environment,
) : EmailSender {
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        check(!logBody || !environment.acceptsProfiles(Profiles.of("json"))) {
            "email.log-body must not be enabled with the json (production) profile: the body carries the token"
        }
    }

    override fun send(
        to: String,
        subject: String,
        body: String,
    ) {
        log.info("email to={} subject={}", to, subject)
        if (logBody) {
            log.info("email body for {}:\n{}", to, body)
        }
    }
}
