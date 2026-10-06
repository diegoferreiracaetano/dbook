package com.dbook.application.identity.bootstrapsuperadminusecase

import com.dbook.application.identity.BootstrapSuperAdminCommand
import com.dbook.application.identity.BootstrapSuperAdminUseCase
import com.dbook.application.identity.loginusecase.FixedPasswordHasher
import com.dbook.application.identity.staff.InMemoryUserRepository
import com.dbook.domain.common.access.Role
import com.dbook.domain.identity.User

abstract class BootstrapSuperAdminUseCaseFixture {
    protected val customer = User(id = 1, email = "customer@example.com", passwordHash = "x", name = "Customer")
    protected open val users = InMemoryUserRepository(customer)
    private val useCase by lazy { BootstrapSuperAdminUseCase(users, FixedPasswordHasher(validPassword = "irrelevant")) }

    protected fun bootstrap(
        email: String = "Root@Example.com",
        password: String = "a-long-passphrase-1",
    ) = useCase.execute(BootstrapSuperAdminCommand(email, password))

    protected fun superAdmin() = users.lockActiveByRole(Role.SUPER_ADMIN).singleOrNull()
}
