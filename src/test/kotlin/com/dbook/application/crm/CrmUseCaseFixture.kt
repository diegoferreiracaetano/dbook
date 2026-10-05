package com.dbook.application.crm

import com.dbook.application.audit.FakeAuditLog
import com.dbook.application.identity.BlockUserUseCase
import com.dbook.application.identity.LoginAttemptGuard
import com.dbook.application.identity.LoginAttemptsPolicy
import com.dbook.application.identity.PasswordConfirmation
import com.dbook.application.identity.UnblockUserUseCase
import com.dbook.application.identity.loginusecase.FakeLoginAttemptLimiter
import com.dbook.application.identity.loginusecase.FakeRefreshTokenRepository
import com.dbook.application.identity.loginusecase.FixedPasswordHasher
import com.dbook.application.identity.staff.InMemoryAnonymizedEmails
import com.dbook.application.identity.staff.InMemoryUserRepository
import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.Role
import com.dbook.domain.identity.User
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

// Shared "given": a SUPER_ADMIN (1), two SUPPORT members (2 and 4) and a customer (3), a frozen clock.
abstract class CrmUseCaseFixture {
    protected val now: Instant = Instant.parse("2026-10-04T12:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    protected val superAdmin = Actor(1, Role.SUPER_ADMIN)
    protected val support = Actor(2, Role.SUPPORT)
    protected val otherSupport = Actor(4, Role.SUPPORT)
    protected val customerId = 3L

    protected val users =
        InMemoryUserRepository(
            User(id = 1, email = "root@example.com", passwordHash = "x", name = "Root", role = Role.SUPER_ADMIN),
            User(id = 2, email = "support@example.com", passwordHash = "x", name = "Support", role = Role.SUPPORT),
            User(id = 3, email = "customer@example.com", passwordHash = "x", name = "Customer"),
            User(id = 4, email = "other@example.com", passwordHash = "x", name = "Other", role = Role.SUPPORT),
        )
    protected val notes = InMemoryCustomerNotes()
    protected val audit = FakeAuditLog()
    protected val refreshTokens = FakeRefreshTokenRepository()
    protected val auditReader = StubAuditLogReader()

    private val guard = CustomerGuard(users)
    protected val addNote = AddCustomerNoteUseCase(guard, notes, audit, clock)
    protected val editNote = EditCustomerNoteUseCase(notes, audit, clock)
    protected val deleteNote = DeleteCustomerNoteUseCase(notes, audit, clock)
    protected val blockCustomer =
        BlockCustomerUseCase(guard, BlockUserUseCase(users, refreshTokens, clock), audit)
    protected val unblockCustomer = UnblockCustomerUseCase(guard, UnblockUserUseCase(users), audit)
    protected val erasure = RecordingCustomerErasure()
    protected val anonymizedEmails = InMemoryAnonymizedEmails()
    private val anonymizer = CustomerAnonymizer(users, refreshTokens, anonymizedEmails, erasure, audit, clock)
    protected val anonymizeCustomer = AnonymizeCustomerUseCase(guard, anonymizer)
    protected val deleteOwnAccount =
        DeleteOwnAccountUseCase(
            users,
            PasswordConfirmation(
                FixedPasswordHasher(validPassword = "current-password-1"),
                LoginAttemptGuard(
                    FakeLoginAttemptLimiter(),
                    LoginAttemptsPolicy(maxFailuresPerEmail = 3, maxFailuresPerIp = 5),
                ),
            ),
            anonymizer,
        )
    protected val getProfile =
        GetCustomerProfileUseCase(FixedCustomerProfileReader(aProfile(3)), auditReader, audit, clock)

    /** A note by [author] on the customer, already stored. */
    protected fun noteBy(
        author: Actor,
        body: String = "called about a refund",
    ) = addNote.execute(AddCustomerNoteCommand(author, customerId, body, pinned = false))
}
