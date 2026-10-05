package com.dbook.presentation.crm

import com.dbook.application.crm.DeleteOwnAccountCommand
import com.dbook.application.crm.DeleteOwnAccountUseCase
import com.dbook.application.crm.ExportMyDataUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** The caller's own rights over their data: `GET /users/me/export` and `DELETE /users/me`. */
@RestController
@RequestMapping("${ApiPaths.V1}/users/me")
@Tag(name = "Users", description = "The authenticated user's own profile")
@SecurityRequirement(name = "bearerAuth")
class MyDataController(
    private val exportMyDataUseCase: ExportMyDataUseCase,
    private val deleteOwnAccountUseCase: DeleteOwnAccountUseCase,
) {
    @Operation(summary = "Everything the system holds about the caller: profile, bookings, payments and reviews")
    @GetMapping("/export")
    fun export(authentication: Authentication): MyDataExportResponse =
        MyDataExportResponse.from(exportMyDataUseCase.execute(authentication.currentUserId()))

    @Operation(summary = "Anonymizes the caller's account for good; needs the password and cannot be undone")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @RequestBody request: DeleteAccountRequest,
        authentication: Authentication,
        servletRequest: HttpServletRequest,
    ) {
        deleteOwnAccountUseCase.execute(
            DeleteOwnAccountCommand(authentication.currentUserId(), request.password, servletRequest.remoteAddr),
        )
    }
}
