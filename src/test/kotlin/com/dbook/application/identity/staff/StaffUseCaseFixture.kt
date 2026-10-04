package com.dbook.application.identity.staff

import com.dbook.application.audit.FakeAuditLog
import com.dbook.application.identity.AcceptInvitationUseCase
import com.dbook.application.identity.AdminPortalLinks
import com.dbook.application.identity.BlockStaffUseCase
import com.dbook.application.identity.BlockUserUseCase
import com.dbook.application.identity.ChangeStaffRoleUseCase
import com.dbook.application.identity.InvitationMailer
import com.dbook.application.identity.InviteStaffUseCase
import com.dbook.application.identity.ListInvitationsUseCase
import com.dbook.application.identity.ResendInvitationUseCase
import com.dbook.application.identity.RevokeInvitationUseCase
import com.dbook.application.identity.StaffSafeguards
import com.dbook.application.identity.UnblockStaffUseCase
import com.dbook.application.identity.UnblockUserUseCase
import com.dbook.application.identity.loginusecase.FakeRefreshTokenRepository
import com.dbook.application.identity.loginusecase.FakeTokenService
import com.dbook.application.identity.loginusecase.FixedPasswordHasher
import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.Role
import com.dbook.domain.identity.StaffInvitation
import com.dbook.domain.identity.User
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

// Shared "given": a SUPER_ADMIN (id 1), a SUPPORT member (id 2) and a customer (id 3), in-memory ports,
// a frozen clock and tokens "token-1", "token-2"... whose stored hash is "hash:<token>".
abstract class StaffUseCaseFixture {
    protected val now: Instant = Instant.parse("2026-10-04T12:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    protected val superAdmin = staffUser(1, "root@example.com", Role.SUPER_ADMIN)
    protected val support = staffUser(2, "support@example.com", Role.SUPPORT)
    protected val customer = User(id = 3, email = "customer@example.com", passwordHash = "hash", name = "Customer")
    protected val actor = Actor(id = 1, role = Role.SUPER_ADMIN)

    protected open val users = InMemoryUserRepository(superAdmin, support, customer)
    protected val invitations = InMemoryInvitationRepository()
    protected val refreshTokens = FakeRefreshTokenRepository()
    protected val audit = FakeAuditLog()
    protected val emails = RecordingEmailSender()
    protected val hasher = FixedPasswordHasher(validPassword = "irrelevant")

    private val tokenService = FakeTokenService()
    private val tokens = SequentialInvitationTokens()
    private val mailer = InvitationMailer(emails, AdminPortalLinks("http://portal.test"))
    private val safeguards by lazy { StaffSafeguards(users) }
    private val blockUser by lazy { BlockUserUseCase(users, refreshTokens, clock) }
    private val unblockUser by lazy { UnblockUserUseCase(users) }

    protected val invite by lazy {
        InviteStaffUseCase(invitations, users, tokens, tokenService, mailer, audit, clock)
    }
    protected val resend by lazy {
        ResendInvitationUseCase(invitations, tokens, tokenService, mailer, audit, clock)
    }
    protected val revoke by lazy { RevokeInvitationUseCase(invitations, audit, clock) }
    protected val listInvitations by lazy { ListInvitationsUseCase(invitations, clock) }
    protected val accept by lazy {
        AcceptInvitationUseCase(invitations, users, tokenService, hasher, audit, clock)
    }
    protected val changeRole by lazy { ChangeStaffRoleUseCase(users, refreshTokens, safeguards, audit) }
    protected val blockStaff by lazy { BlockStaffUseCase(blockUser, safeguards, audit) }
    protected val unblockStaff by lazy { UnblockStaffUseCase(unblockUser, safeguards, audit) }

    protected fun staffUser(
        id: Long,
        email: String,
        role: Role,
    ) = User(id = id, email = email, passwordHash = "hash", name = email.substringBefore("@"), role = role)

    /** An invitation already stored, as if it had been sent: its token is [token] (the stored hash is derived). */
    protected fun seedInvitation(
        email: String = "new@example.com",
        token: String = "abc",
        role: Role = Role.SUPPORT,
    ): StaffInvitation =
        invitations.save(StaffInvitation.issue(email, role, tokenService.hashToken(token), invitedBy = 1, now = now))

    /** Runs [action] and then the after-commit callbacks, the way a successful commit would. */
    protected fun <T> committed(action: () -> T): T {
        TransactionSynchronizationManager.initSynchronization()
        try {
            val result = action()
            TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }
            return result
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    /** Runs [action] but never the after-commit callbacks: the transaction "rolled back". */
    protected fun <T> rolledBack(action: () -> T): T {
        TransactionSynchronizationManager.initSynchronization()
        try {
            return action()
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }
}
