package com.dbook.application.identity.staff

import com.dbook.domain.common.access.Role
import com.dbook.domain.identity.AnonymizedEmailRepository
import com.dbook.domain.identity.EmailSender
import com.dbook.domain.identity.InvitationTokenGenerator
import com.dbook.domain.identity.StaffInvitation
import com.dbook.domain.identity.StaffInvitationRepository
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserRepository
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryUserRepository(vararg initial: User) : UserRepository {
    private val users = initial.toMutableList()
    private var lastId = users.maxOfOrNull { it.id ?: 0 } ?: 0

    override fun findById(id: Long): User? = users.find { it.id == id }

    override fun findByEmail(email: String): User? = users.find { it.email.equals(email, ignoreCase = true) }

    override fun save(user: User): User {
        val stored =
            if (user.id == null) {
                User(
                    ++lastId, user.email, user.passwordHash, user.name, user.role, user.status, user.blockedReason,
                    user.blockedAt, user.lastLoginAt, user.anonymizedAt, user.emailVerifiedAt, user.version,
                )
            } else {
                user
            }
        users.removeAll { it.id == stored.id }
        users += stored
        return stored
    }

    override fun recordLogin(
        userId: Long,
        at: Instant,
    ) = Unit

    override fun findStaff(): List<User> = users.filter { it.role.isStaff }.sortedBy { it.id }

    override fun existsByRole(role: Role): Boolean = users.any { it.role == role }

    override fun lockActiveByRole(role: Role): List<User> =
        users.filter { it.role == role && !it.isBlocked }.sortedBy { it.id }
}

class InMemoryAnonymizedEmails : AnonymizedEmailRepository {
    private val hashes = mutableSetOf<String>()

    override fun remember(email: String) {
        hashes += email.trim().lowercase()
    }

    override fun isRemembered(email: String): Boolean = email.trim().lowercase() in hashes
}

class InMemoryInvitationRepository : StaffInvitationRepository {
    private val invitations = mutableListOf<StaffInvitation>()
    private var lastId = 0L

    override fun save(invitation: StaffInvitation): StaffInvitation {
        val stored =
            if (invitation.id == null) {
                with(invitation) {
                    StaffInvitation(
                        ++lastId, email, role, tokenHash, invitedBy, expiresAt, createdAt, acceptedAt, revokedAt,
                    )
                }
            } else {
                invitation
            }
        invitations.removeAll { it.id == stored.id }
        invitations += stored
        return stored
    }

    override fun findById(id: Long): StaffInvitation? = invitations.find { it.id == id }

    override fun findByTokenHash(tokenHash: String): StaffInvitation? = invitations.find { it.tokenHash == tokenHash }

    override fun findOpenByEmail(email: String): StaffInvitation? = invitations.find { it.email == email && it.isOpen }

    override fun findRecent(): List<StaffInvitation> = invitations.sortedByDescending { it.createdAt }
}

class SequentialInvitationTokens : InvitationTokenGenerator {
    private var count = 0

    override fun generate(): String = "token-${++count}"
}

class RecordingEmailSender : EmailSender {
    data class Mail(val to: String, val subject: String, val body: String)

    val sent = CopyOnWriteArrayList<Mail>()
    var failWith: RuntimeException? = null

    override fun send(
        to: String,
        subject: String,
        body: String,
    ) {
        failWith?.let { throw it }
        sent += Mail(to, subject, body)
    }
}
