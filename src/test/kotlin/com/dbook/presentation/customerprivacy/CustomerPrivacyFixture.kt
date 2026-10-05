package com.dbook.presentation.customerprivacy

import com.dbook.presentation.customernotes.CustomerNotesFixture
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch

abstract class CustomerPrivacyFixture : CustomerNotesFixture() {
    protected fun superAdminToken() = staffToken(com.dbook.domain.identity.Role.SUPER_ADMIN)

    /** GET /v1/admin/customers/export; the body is streamed, so the response is read after the async dispatch. */
    protected fun exportCsv(
        token: String,
        vararg params: Pair<String, String>,
    ): MvcResult {
        val started =
            mockMvc.get("/v1/admin/customers/export") {
                header("Authorization", "Bearer $token")
                params.forEach { (name, value) -> param(name, value) }
            }.andReturn()
        return if (started.request.isAsyncStarted) mockMvc.perform(asyncDispatch(started)).andReturn() else started
    }

    protected fun csvOf(result: MvcResult): List<String> =
        result.response.getContentAsString(Charsets.UTF_8).removePrefix("\uFEFF").trim().lines()

    protected fun anonymize(
        token: String,
        customerId: Long,
        reason: String = "Erasure requested by the customer",
        confirmation: String = "ANONYMIZE $customerId",
    ): MvcResult =
        mockMvc.post("/v1/admin/customers/$customerId/anonymize") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("reason" to reason, "confirmation" to confirmation))
        }.andReturn()

    protected fun myExport(token: String): MvcResult =
        mockMvc.get("/v1/users/me/export") { header("Authorization", "Bearer $token") }.andReturn()

    protected fun deleteMyAccount(
        token: String,
        password: String,
    ): MvcResult =
        mockMvc.delete("/v1/users/me") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("password" to password))
        }.andReturn()

    protected fun registerStatus(email: String): Int =
        mockMvc.post("/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(
                    mapOf("email" to email, "password" to "s3cret-password", "name" to "Someone"),
                )
        }.andReturn().response.status

    protected fun countOf(
        table: String,
        column: String,
        id: Long,
    ): Int = jdbcTemplate.queryForObject("SELECT count(*) FROM $table WHERE $column = ?", Int::class.java, id) ?: 0
}
