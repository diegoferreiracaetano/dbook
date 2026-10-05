package com.dbook.presentation.crm

import com.dbook.application.crm.ExportCustomersUseCase
import com.dbook.application.crm.GetCustomerProfileUseCase
import com.dbook.application.crm.ListCustomerHistoryUseCase
import com.dbook.application.crm.SearchCustomersUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.PageParams
import com.dbook.presentation.common.PageResponse
import com.dbook.presentation.common.currentActor
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

/** `/admin/customers` — the CRM: finding customers and reading what the team knows about them. */
@RestController
@RequestMapping("${ApiPaths.V1}/admin/customers")
@Tag(name = "Customers (admin)", description = "The CRM: finding and reading customers")
@SecurityRequirement(name = "bearerAuth")
class CustomerController(
    private val searchCustomersUseCase: SearchCustomersUseCase,
    private val getCustomerProfileUseCase: GetCustomerProfileUseCase,
    private val listCustomerHistoryUseCase: ListCustomerHistoryUseCase,
    private val exportCustomersUseCase: ExportCustomersUseCase,
) {
    @Operation(summary = "Searches customers by text, status, period and bookings, one page at a time (CUSTOMER_READ)")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @GetMapping
    fun search(request: CustomerSearchRequest): PageResponse<CustomerSummaryResponse> =
        PageResponse.from(searchCustomersUseCase.execute(request.toQuery()), CustomerSummaryResponse::from)

    @Operation(summary = "The full picture of one customer: profile, bookings, payments and reviews (CUSTOMER_READ)")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @GetMapping("/{id}")
    fun profile(
        @PathVariable id: Long,
        authentication: Authentication,
    ): CustomerProfileResponse =
        CustomerProfileResponse.from(getCustomerProfileUseCase.execute(authentication.currentActor(), id))

    @Operation(summary = "A customer's bookings, newest first, with the frozen price and the flight (CUSTOMER_READ)")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @GetMapping("/{id}/bookings")
    fun bookings(
        @PathVariable id: Long,
        params: PageParams,
    ): PageResponse<CustomerBookingResponse> =
        PageResponse.from(listCustomerHistoryUseCase.bookings(id, params.toQuery()), CustomerBookingResponse::from)

    @Operation(summary = "A customer's payments, newest first, with the card's last four digits (CUSTOMER_READ)")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @GetMapping("/{id}/payments")
    fun payments(
        @PathVariable id: Long,
        params: PageParams,
    ): PageResponse<CustomerPaymentResponse> =
        PageResponse.from(listCustomerHistoryUseCase.payments(id, params.toQuery()), CustomerPaymentResponse::from)

    @Operation(summary = "A customer's reviews, newest first (CUSTOMER_READ)")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @GetMapping("/{id}/reviews")
    fun reviews(
        @PathVariable id: Long,
        params: PageParams,
    ): PageResponse<CustomerReviewResponse> =
        PageResponse.from(listCustomerHistoryUseCase.reviews(id, params.toQuery()), CustomerReviewResponse::from)

    @Operation(
        summary = "Downloads the filtered customers as CSV, at most 50 000 rows, and records it (CUSTOMER_EXPORT)",
    )
    @PreAuthorize("hasAuthority('CUSTOMER_EXPORT')")
    @GetMapping("/export")
    fun export(
        request: CustomerExportRequest,
        authentication: Authentication,
    ): ResponseEntity<StreamingResponseBody> {
        val export = exportCustomersUseCase.execute(request.toCommand(authentication.currentActor()))
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"customers.csv\"")
            .body(StreamingResponseBody { out -> CustomerCsv.write(out, export) })
    }
}
