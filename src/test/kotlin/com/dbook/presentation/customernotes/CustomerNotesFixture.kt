package com.dbook.presentation.customernotes

import com.dbook.domain.common.access.Role
import com.dbook.presentation.customerprofile.CustomerProfileFixture
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

abstract class CustomerNotesFixture : CustomerProfileFixture() {
    protected fun staffToken(role: Role = Role.SUPPORT): String = registerStaffAndLogin(uniqueEmail(), role)

    protected fun newCustomerId(): Long {
        val email = uniqueEmail()
        registerAndLogin(email)
        return userIdOf(email)
    }

    private fun json(body: Map<String, Any?>) = objectMapper.writeValueAsString(body)

    protected fun addNote(
        token: String,
        customerId: Long,
        body: String,
        pinned: Boolean = false,
    ): MvcResult =
        mockMvc.post("/v1/admin/customers/$customerId/notes") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("body" to body, "pinned" to pinned))
        }.andReturn()

    protected fun noteIdOf(created: MvcResult): Long = bodyOf(created)["id"].asLong()

    protected fun editNote(
        token: String,
        customerId: Long,
        noteId: Long,
        body: Map<String, Any?>,
    ): MvcResult =
        mockMvc.patch("/v1/admin/customers/$customerId/notes/$noteId") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = json(body)
        }.andReturn()

    protected fun deleteNote(
        token: String,
        customerId: Long,
        noteId: Long,
    ): MvcResult =
        mockMvc.delete("/v1/admin/customers/$customerId/notes/$noteId") {
            header("Authorization", "Bearer $token")
        }.andReturn()

    protected fun listNotes(
        token: String,
        customerId: Long,
    ): MvcResult =
        mockMvc.get("/v1/admin/customers/$customerId/notes") { header("Authorization", "Bearer $token") }.andReturn()

    protected fun blockCustomer(
        token: String,
        customerId: Long,
        reason: String,
    ): MvcResult =
        mockMvc.post("/v1/admin/customers/$customerId/block") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("reason" to reason))
        }.andReturn()

    protected fun unblockCustomer(
        token: String,
        customerId: Long,
    ): MvcResult =
        mockMvc.post("/v1/admin/customers/$customerId/unblock") { header("Authorization", "Bearer $token") }.andReturn()

    protected fun loginStatus(email: String): Int =
        mockMvc.post("/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("email" to email, "password" to "s3cret-password"))
        }.andReturn().response.status

    protected fun refreshStatus(refreshToken: String): Int =
        mockMvc.post("/v1/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("refreshToken" to refreshToken))
        }.andReturn().response.status
}
