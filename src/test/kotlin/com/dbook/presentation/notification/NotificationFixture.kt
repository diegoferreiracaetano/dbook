package com.dbook.presentation.notification

import com.dbook.application.identity.staff.RecordingEmailSender
import com.dbook.application.notification.NotificationEvent
import com.dbook.application.notification.ProcessNotificationEventUseCase
import com.dbook.domain.notification.NotificationType
import com.dbook.presentation.securityintegration.SecurityIntegrationFixture
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.util.UUID

// The real use case and the real tables: what a consumer would do with a message, then what the customer's app asks.
abstract class NotificationFixture : SecurityIntegrationFixture() {
    @Autowired
    lateinit var processNotificationEventUseCase: ProcessNotificationEventUseCase

    @Autowired
    lateinit var emailSender: RecordingEmailSender

    /** A customer with an account; returns their token and their id. */
    protected fun aCustomer(): Pair<String, Long> {
        val email = uniqueEmail()
        return registerAndLogin(email) to userIdOf(email)
    }

    protected fun deliver(
        customerId: Long,
        type: NotificationType = NotificationType.BOOKING_CONFIRMED,
        eventId: UUID = UUID.randomUUID(),
    ) = processNotificationEventUseCase.execute(
        NotificationEvent(eventId, type, mapOf("customerId" to customerId, "bookingId" to 1, "title" to "GRU-GIG")),
    )

    protected fun body(result: MvcResult): JsonNode =
        objectMapper.readTree(
            result.response.getContentAsString(Charsets.UTF_8),
        )

    protected fun inbox(
        token: String,
        vararg params: Pair<String, String>,
    ): MvcResult =
        mockMvc.get("/v1/notifications") {
            header("Authorization", "Bearer $token")
            params.forEach { (name, value) -> param(name, value) }
        }.andReturn()

    protected fun unreadCount(token: String): Long =
        body(
            mockMvc.get("/v1/notifications/unread-count") { header("Authorization", "Bearer $token") }.andReturn(),
        )["count"].asLong()

    protected fun markRead(
        token: String,
        id: Long,
    ): MvcResult = mockMvc.post("/v1/notifications/$id/read") { header("Authorization", "Bearer $token") }.andReturn()

    protected fun markAllRead(token: String): MvcResult =
        mockMvc.post("/v1/notifications/read-all") { header("Authorization", "Bearer $token") }.andReturn()

    protected fun preferences(token: String): MvcResult =
        mockMvc.get("/v1/notifications/preferences") { header("Authorization", "Bearer $token") }.andReturn()

    protected fun updatePreferences(
        token: String,
        changes: List<Map<String, Any>>,
    ): MvcResult =
        mockMvc.put("/v1/notifications/preferences") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(changes)
        }.andReturn()

    protected fun registerDevice(
        token: String,
        deviceToken: String,
    ): MvcResult =
        mockMvc.post("/v1/notifications/devices") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("token" to deviceToken, "platform" to "ANDROID"))
        }.andReturn()

    protected fun unregisterDevice(
        token: String,
        deviceToken: String,
    ): MvcResult =
        mockMvc.delete(
            "/v1/notifications/devices/$deviceToken",
        ) { header("Authorization", "Bearer $token") }.andReturn()

    protected fun count(
        sql: String,
        vararg args: Any,
    ): Int = jdbcTemplate.queryForObject(sql, Int::class.java, *args) ?: 0
}
