package com.dbook.application.identity

import com.dbook.domain.audit.AuditAction
import com.dbook.domain.audit.AuditEvent
import com.dbook.domain.audit.AuditLog
import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.RefreshTokenRepository
import com.dbook.domain.identity.Role
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserRepository
import com.dbook.domain.identity.toAuditSnapshot
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class ChangeStaffRoleCommand(
    val actor: Actor,
    val targetId: Long,
    val newRole: Role,
)

@Observed(name = "dbook.usecase")
@Service
class ChangeStaffRoleUseCase(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val safeguards: StaffSafeguards,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(command: ChangeStaffRoleCommand): User {
        require(command.newRole.isStaff) {
            "A staff member can only be given a staff role; block the account to remove its access"
        }
        safeguards.requireNotSelf(command.actor, command.targetId)
        val target = safeguards.staffMember(command.targetId)
        if (target.role == command.newRole) {
            return target
        }
        safeguards.requireSuperAdminRemainsWithout(target)

        val changed = userRepository.save(target.changeRole(command.newRole))
        // the sessions end with the old role: the person logs in again and gets the new one
        refreshTokenRepository.revokeAllForUser(command.targetId)
        auditLog.record(
            AuditEvent(
                actor = command.actor,
                action = AuditAction.STAFF_ROLE_CHANGED,
                targetId = command.targetId.toString(),
                before = target.toAuditSnapshot(),
                after = changed.toAuditSnapshot(),
            ),
        )
        return changed
    }
}
