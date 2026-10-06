package com.dbook.presentation.crm

import com.dbook.application.crm.AddCustomerNoteCommand
import com.dbook.application.crm.AddCustomerNoteUseCase
import com.dbook.application.crm.DeleteCustomerNoteCommand
import com.dbook.application.crm.DeleteCustomerNoteUseCase
import com.dbook.application.crm.EditCustomerNoteCommand
import com.dbook.application.crm.EditCustomerNoteUseCase
import com.dbook.application.crm.ListCustomerNotesUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.PageParams
import com.dbook.presentation.common.PageResponse
import com.dbook.presentation.common.currentActor
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** `/admin/customers/{customerId}/notes` — what the team knows about a customer that is not in the data. */
@RestController
@RequestMapping("${ApiPaths.V1}/admin/customers/{customerId}/notes")
@Tag(name = "Customer notes (admin)", description = "Internal notes about a customer")
@SecurityRequirement(name = "bearerAuth")
class CustomerNoteController(
    private val addCustomerNoteUseCase: AddCustomerNoteUseCase,
    private val editCustomerNoteUseCase: EditCustomerNoteUseCase,
    private val deleteCustomerNoteUseCase: DeleteCustomerNoteUseCase,
    private val listCustomerNotesUseCase: ListCustomerNotesUseCase,
) {
    @Operation(summary = "Lists the notes of a customer, pinned first then newest (CUSTOMER_READ)")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @GetMapping
    fun list(
        @PathVariable customerId: Long,
        params: PageParams,
    ): PageResponse<CustomerNoteResponse> =
        PageResponse.from(listCustomerNotesUseCase.execute(customerId, params.toQuery()), CustomerNoteResponse::from)

    @Operation(summary = "Writes a note about a customer (CUSTOMER_NOTE)")
    @PreAuthorize("hasAuthority('CUSTOMER_NOTE')")
    @PostMapping
    fun add(
        @PathVariable customerId: Long,
        @RequestBody request: WriteCustomerNoteRequest,
        authentication: Authentication,
    ): ResponseEntity<CustomerNoteResponse> {
        val command = AddCustomerNoteCommand(authentication.currentActor(), customerId, request.body, request.pinned)
        return ResponseEntity.status(
            HttpStatus.CREATED,
        ).body(CustomerNoteResponse.from(addCustomerNoteUseCase.execute(command)))
    }

    @Operation(summary = "Edits the text or the pin of your own note (CUSTOMER_NOTE)")
    @PreAuthorize("hasAuthority('CUSTOMER_NOTE')")
    @PatchMapping("/{noteId}")
    fun edit(
        @PathVariable customerId: Long,
        @PathVariable noteId: Long,
        @RequestBody request: EditCustomerNoteRequest,
        authentication: Authentication,
    ): CustomerNoteResponse =
        CustomerNoteResponse.from(
            editCustomerNoteUseCase.execute(
                EditCustomerNoteCommand(
                    authentication.currentActor(),
                    customerId,
                    noteId,
                    request.body,
                    request.pinned,
                ),
            ),
        )

    @Operation(summary = "Deletes a note: your own, or anyone's if you are a SUPER_ADMIN (CUSTOMER_NOTE)")
    @PreAuthorize("hasAuthority('CUSTOMER_NOTE')")
    @DeleteMapping("/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable customerId: Long,
        @PathVariable noteId: Long,
        authentication: Authentication,
    ) {
        deleteCustomerNoteUseCase.execute(DeleteCustomerNoteCommand(authentication.currentActor(), customerId, noteId))
    }
}
