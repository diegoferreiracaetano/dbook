package com.dbook.application.identity.registeruserusecase

import com.dbook.application.identity.AccountLinkIssuer
import com.dbook.application.identity.AccountMailer
import com.dbook.application.identity.AdminPortalLinks
import com.dbook.application.identity.CustomerAppLinks
import com.dbook.application.identity.RegisterUserCommand
import com.dbook.application.identity.RegisterUserUseCase
import com.dbook.application.identity.accountrecovery.InMemoryAccountTokens
import com.dbook.application.identity.loginusecase.FakeTokenService
import com.dbook.application.identity.staff.InMemoryAnonymizedEmails
import com.dbook.application.identity.staff.RecordingEmailSender
import com.dbook.application.identity.staff.SequentialInvitationTokens
import com.dbook.domain.identity.PasswordHasher
import com.dbook.domain.identity.Role
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserRepository
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class FakeUserRepository : UserRepository {
    val users = mutableListOf<User>()

    override fun findById(id: Long): User? = users.find { it.id == id }

    override fun findByEmail(email: String): User? = users.find { it.email == email }

    override fun save(user: User): User {
        val saved =
            User(
                id = user.id ?: (users.size + 1L),
                email = user.email,
                passwordHash = user.passwordHash,
                name = user.name,
                role = user.role,
            )
        users += saved
        return saved
    }

    override fun recordLogin(
        userId: Long,
        at: Instant,
    ) = error("not needed for this test")

    override fun findStaff(): List<User> = error("not needed for this test")

    override fun existsByRole(role: Role): Boolean = error("not needed for this test")

    override fun lockActiveByRole(role: Role): List<User> = error("not needed for this test")
}

class FakePasswordHasher : PasswordHasher {
    override fun hash(rawPassword: String): String = "hashed:$rawPassword"

    override fun matches(
        rawPassword: String,
        hash: String,
    ): Boolean = hash == "hashed:$rawPassword"
}

abstract class RegisterUserUseCaseFixture {
    protected val userRepository = FakeUserRepository()
    protected val anonymizedEmails = InMemoryAnonymizedEmails()
    private val tokens = InMemoryAccountTokens { Instant.parse("2026-10-05T12:00:00Z") }
    protected val mailbox = RecordingEmailSender()
    private val useCase =
        RegisterUserUseCase(
            userRepository,
            FakePasswordHasher(),
            anonymizedEmails,
            AccountLinkIssuer(
                tokens,
                SequentialInvitationTokens(),
                FakeTokenService(),
                Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneOffset.UTC),
            ),
            AccountMailer(mailbox, CustomerAppLinks("https://app.test"), AdminPortalLinks("https://portal.test")),
        )

    // the use case runs inside a transaction in production: the mail is sent after it commits
    protected fun register(command: RegisterUserCommand): User {
        TransactionSynchronizationManager.initSynchronization()
        try {
            val saved = useCase.execute(command)
            TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }
            return saved
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }
}
