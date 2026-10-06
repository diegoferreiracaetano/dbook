package com.dbook.presentation.crm

import com.dbook.application.crm.AnonymizeCustomerCommand
import com.dbook.application.crm.AnonymizeCustomerUseCase
import com.dbook.application.crm.BlockCustomerCommand
import com.dbook.application.crm.BlockCustomerUseCase
import com.dbook.application.crm.UnblockCustomerCommand
import com.dbook.application.crm.UnblockCustomerUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.currentActor
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `/admin/customers/{id}/block|unblock` — closing and reopening a customer's access. */
@RestController
@RequestMapping("${ApiPaths.V1}/admin/customers/{id}")
@Tag(name = "Customers (admin)", description = "The CRM: finding and reading customers")
@SecurityRequirement(name = "bearerAuth")
class CustomerAccountController(
    private val blockCustomerUseCase: BlockCustomerUseCase,
    private val unblockCustomerUseCase: UnblockCustomerUseCase,
    private val anonymizeCustomerUseCase: AnonymizeCustomerUseCase,
) {
    @Operation(summary = "Blocks a customer and ends their sessions; the reason is required (CUSTOMER_BLOCK)")
    @PreAuthorize("hasAuthority('CUSTOMER_BLOCK')")
    @PostMapping("/block")
    fun block(
        @PathVariable id: Long,
        @RequestBody request: BlockCustomerRequest,
        authentication: Authentication,
    ): CustomerAccountResponse =
        CustomerAccountResponse.from(
            blockCustomerUseCase.execute(BlockCustomerCommand(authentication.currentActor(), id, request.reason)),
        )

    @Operation(summary = "Lets a blocked customer log in again (CUSTOMER_BLOCK)")
    @PreAuthorize("hasAuthority('CUSTOMER_BLOCK')")
    @PostMapping("/unblock")
    fun unblock(
        @PathVariable id: Long,
        authentication: Authentication,
    ): CustomerAccountResponse =
        CustomerAccountResponse.from(
            unblockCustomerUseCase.execute(UnblockCustomerCommand(authentication.currentActor(), id)),
        )

    @Operation(summary = "Anonymizes a customer for good: needs a reason and the confirmation phrase (CUSTOMER_ERASE)")
    @PreAuthorize("hasAuthority('CUSTOMER_ERASE')")
    @PostMapping("/anonymize")
    fun anonymize(
        @PathVariable id: Long,
        @RequestBody request: AnonymizeCustomerRequest,
        authentication: Authentication,
    ): CustomerAccountResponse =
        CustomerAccountResponse.from(
            anonymizeCustomerUseCase.execute(
                AnonymizeCustomerCommand(authentication.currentActor(), id, request.reason, request.confirmation),
            ),
        )
}
