package com.dbook.application.identity.changepasswordusecase

import com.dbook.application.identity.ChangePasswordCommand
import com.dbook.application.identity.ChangePasswordUseCase
import com.dbook.application.identity.LoginAttemptGuard
import com.dbook.application.identity.LoginAttemptsPolicy
import com.dbook.application.identity.loginusecase.FakeLoginAttemptLimiter
import com.dbook.application.identity.loginusecase.FakeRefreshTokenRepository
import com.dbook.application.identity.loginusecase.FixedPasswordHasher
import com.dbook.application.identity.staff.InMemoryUserRepository
import com.dbook.domain.identity.Role
import com.dbook.domain.identity.User

// Shared "given": a SUPER_ADMIN (id 1) and a customer (id 2), both with the current password "current-password-1";
// three wrong attempts lock the account.
abstract class ChangePasswordUseCaseFixture {
    protected val users =
        InMemoryUserRepository(
            User(id = 1, email = "root@example.com", passwordHash = "old", name = "Root", role = Role.SUPER_ADMIN),
            User(id = 2, email = "customer@example.com", passwordHash = "old", name = "Customer"),
        )
    protected val refreshTokens = FakeRefreshTokenRepository()
    private val guard =
        LoginAttemptGuard(FakeLoginAttemptLimiter(), LoginAttemptsPolicy(maxFailuresPerEmail = 3, maxFailuresPerIp = 5))
    private val useCase =
        ChangePasswordUseCase(users, FixedPasswordHasher(validPassword = "current-password-1"), refreshTokens, guard)

    protected fun change(
        userId: Long = 1,
        current: String = "current-password-1",
        new: String = "a-brand-new-passphrase",
    ) = useCase.execute(ChangePasswordCommand(userId, current, new, clientIp = "10.0.0.1"))
}
