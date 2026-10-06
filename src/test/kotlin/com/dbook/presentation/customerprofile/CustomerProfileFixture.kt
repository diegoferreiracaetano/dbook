package com.dbook.presentation.customerprofile

import com.dbook.domain.common.access.Role
import com.dbook.presentation.customersearch.CustomerSearchFixture
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID

// Builds a customer's history through the real endpoints (book, pay, cancel, review), then reads it back as staff.
abstract class CustomerProfileFixture : CustomerSearchFixture() {
    /** Books [count] seats, each on its own flight, and returns the booking ids. */
    protected fun bookSeats(
        token: String,
        count: Int,
    ): List<Long> =
        List(count) {
            val (bookableId, seatId) = registerFlightWithOneSeat()
            book(token, bookableId, seatId)
        }

    protected fun pay(
        token: String,
        bookingId: Long,
    ) {
        mockMvc.post("/v1/payments") {
            header("Authorization", "Bearer $token")
            header("Idempotency-Key", UUID.randomUUID().toString())
            contentType = MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(
                    mapOf("bookingIds" to listOf(bookingId), "cardLast4" to "4242", "cardholderName" to "Jane Doe"),
                )
        }.andExpect { status { isCreated() } }
    }

    protected fun cancel(
        token: String,
        bookingId: Long,
    ) {
        mockMvc.post("/v1/bookings/$bookingId/cancel") { header("Authorization", "Bearer $token") }
            .andExpect { status { isOk() } }
    }

    protected fun review(
        token: String,
        bookingId: Long,
        rating: Int,
    ) {
        mockMvc.post("/v1/reviews") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(mapOf("bookingId" to bookingId, "rating" to rating, "comment" to "ok"))
        }.andExpect { status { isCreated() } }
    }

    protected fun profile(
        staffToken: String,
        customerId: Long,
    ): MvcResult =
        mockMvc.get("/v1/admin/customers/$customerId") {
            header("Authorization", "Bearer $staffToken")
        }.andReturn()

    // The code is random, so it can collide with an airport another test created: try another one until it is taken
    protected fun newDestination(): String {
        val staff = registerStaffAndLogin(uniqueEmail(), Role.CATALOG_MANAGER)
        repeat(ATTEMPTS) {
            val code = ('A'..'Z').shuffled().take(IATA_LENGTH).joinToString("")
            val created =
                mockMvc.post("/v1/admin/airports") {
                    header("Authorization", "Bearer $staff")
                    contentType = MediaType.APPLICATION_JSON
                    content =
                        objectMapper.writeValueAsString(
                            mapOf(
                                "iataCode" to code, "name" to "Review Field", "city" to "Reviewville",
                                "country" to "Brasil", "photoUrl" to "https://example.com/r.jpg",
                                "region" to "América do Sul", "isPopular" to true,
                            ),
                        )
                }.andReturn()
            if (created.response.status == HttpStatus.CREATED.value()) return code
        }
        error("could not find a free IATA code")
    }

    private companion object {
        const val IATA_LENGTH = 3
        const val ATTEMPTS = 20
    }
}
