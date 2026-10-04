package com.dbook

import com.dbook.application.identity.staff.RecordingEmailSender
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

// Imported by AbstractIntegrationTest so every test shares ONE context: the mail "sent" by an invitation is
// captured here instead of being logged, and the test reads the link out of it, as the invitee would.
@TestConfiguration
class RecordingEmailConfig {
    @Bean
    @Primary
    fun recordingEmailSender() = RecordingEmailSender()
}
