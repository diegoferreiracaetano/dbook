package com.dbook.presentation.identity.bootstrapsuperadminrunner

import com.dbook.application.identity.BootstrapSuperAdminUseCase
import com.dbook.application.identity.loginusecase.FixedPasswordHasher
import com.dbook.application.identity.staff.InMemoryUserRepository
import com.dbook.domain.identity.Role
import com.dbook.presentation.identity.BootstrapSuperAdminRunner
import org.springframework.boot.DefaultApplicationArguments

abstract class BootstrapSuperAdminRunnerFixture {
    protected val users = InMemoryUserRepository()
    private val useCase = BootstrapSuperAdminUseCase(users, FixedPasswordHasher(validPassword = "irrelevant"))

    protected fun runWith(
        email: String,
        password: String,
    ) = BootstrapSuperAdminRunner(useCase, email, password).run(DefaultApplicationArguments())

    protected fun hasSuperAdmin() = users.existsByRole(Role.SUPER_ADMIN)
}
