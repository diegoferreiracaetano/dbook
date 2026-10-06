package com.dbook.presentation.identity

import com.dbook.application.identity.BootstrapSuperAdminCommand
import com.dbook.application.identity.BootstrapSuperAdminUseCase
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * Inbound adapter, like a controller: at startup, creates the first SUPER_ADMIN from the environment, but only
 * while none exists. Setting just one of the two variables is a misconfiguration and stops the startup.
 */
@Component
class BootstrapSuperAdminRunner(
    private val bootstrapSuperAdminUseCase: BootstrapSuperAdminUseCase,
    @Value("\${bootstrap-admin.email}") private val email: String,
    @Value("\${bootstrap-admin.password}") private val password: String,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        if (email.isBlank() && password.isBlank()) {
            return
        }
        check(email.isNotBlank() && password.isNotBlank()) {
            "bootstrap-admin.email and bootstrap-admin.password must be set together"
        }
        if (bootstrapSuperAdminUseCase.execute(BootstrapSuperAdminCommand(email, password))) {
            log.info("Bootstrap SUPER_ADMIN created for {}", email)
        }
    }
}
