package com.dbook.presentation.audit

import com.dbook.application.audit.SearchAuditLogUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.CursorPageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `GET /admin/audit` — the trail of administrative actions, newest first. */
@RestController
@RequestMapping("${ApiPaths.V1}/admin/audit")
@Tag(name = "Audit (admin)", description = "Trail of administrative actions")
@SecurityRequirement(name = "bearerAuth")
class AuditController(
    private val searchAuditLogUseCase: SearchAuditLogUseCase,
) {
    @Operation(summary = "Lists audit entries, newest first, one page at a time (needs AUDIT_READ)")
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    @GetMapping
    fun search(request: AuditSearchRequest): CursorPageResponse<AuditEntryResponse> {
        val page = searchAuditLogUseCase.execute(request.toQuery())
        return CursorPageResponse(
            items = page.entries.map(AuditEntryResponse::from),
            nextCursor = page.next?.let(AuditCursorCodec::encode),
        )
    }
}
