package com.dbook.presentation.identity

import com.dbook.application.identity.BlockStaffCommand
import com.dbook.application.identity.BlockStaffUseCase
import com.dbook.application.identity.ChangeStaffRoleCommand
import com.dbook.application.identity.ChangeStaffRoleUseCase
import com.dbook.application.identity.ListStaffUseCase
import com.dbook.application.identity.UnblockStaffCommand
import com.dbook.application.identity.UnblockStaffUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.currentActor
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `/admin/staff` — the team: listing, changing roles, blocking. All of it needs ADMIN_MANAGE. */
@RestController
@RequestMapping("${ApiPaths.V1}/admin/staff")
@Tag(name = "Staff (admin)", description = "The team: roles and access")
@SecurityRequirement(name = "bearerAuth")
class StaffController(
    private val listStaffUseCase: ListStaffUseCase,
    private val changeStaffRoleUseCase: ChangeStaffRoleUseCase,
    private val blockStaffUseCase: BlockStaffUseCase,
    private val unblockStaffUseCase: UnblockStaffUseCase,
) {
    @Operation(summary = "Lists the staff, blocked ones included")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE')")
    @GetMapping
    fun list(): List<StaffMemberResponse> = listStaffUseCase.execute().map(StaffMemberResponse::from)

    @Operation(
        summary = "Changes a staff member's role and ends their sessions (never your own, never the last SUPER_ADMIN)",
    )
    @PreAuthorize("hasAuthority('ADMIN_MANAGE')")
    @PatchMapping("/{id}/role")
    fun changeRole(
        @PathVariable id: Long,
        @RequestBody request: ChangeStaffRoleRequest,
        authentication: Authentication,
    ): StaffMemberResponse =
        StaffMemberResponse.from(
            changeStaffRoleUseCase.execute(ChangeStaffRoleCommand(authentication.currentActor(), id, request.role)),
        )

    @Operation(summary = "Blocks a staff member and ends their sessions (never yourself, never the last SUPER_ADMIN)")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE')")
    @PostMapping("/{id}/block")
    fun block(
        @PathVariable id: Long,
        @RequestBody request: BlockStaffRequest,
        authentication: Authentication,
    ): StaffMemberResponse =
        StaffMemberResponse.from(
            blockStaffUseCase.execute(BlockStaffCommand(authentication.currentActor(), id, request.reason)),
        )

    @Operation(summary = "Lets a blocked staff member log in again")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE')")
    @PostMapping("/{id}/unblock")
    fun unblock(
        @PathVariable id: Long,
        authentication: Authentication,
    ): StaffMemberResponse =
        StaffMemberResponse.from(unblockStaffUseCase.execute(UnblockStaffCommand(authentication.currentActor(), id)))
}
