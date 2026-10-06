package com.dbook.presentation.staffmanagement

import com.dbook.application.identity.staff.RecordingEmailSender
import com.dbook.domain.common.access.Role
import com.dbook.presentation.securityintegration.SecurityIntegrationFixture
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

// Drives the invitation and team endpoints as a real SUPER_ADMIN would, and reads the invitation link out of the
// captured e-mail, since the token exists nowhere else.
abstract class StaffManagementFixture : SecurityIntegrationFixture() {
    @Autowired
    lateinit var emails: RecordingEmailSender

    protected fun newSuperAdminToken(): String = registerStaffAndLogin(uniqueEmail())

    protected fun invite(
        adminToken: String,
        email: String,
        role: Role = Role.SUPPORT,
    ): ResultActionsDsl =
        mockMvc.post("/v1/admin/invitations") {
            header("Authorization", "Bearer $adminToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("email" to email, "role" to role.name))
        }

    protected fun idOf(result: ResultActionsDsl): Long = json(result.andReturn())["id"].asLong()

    protected fun json(result: MvcResult): JsonNode = objectMapper.readTree(result.response.contentAsString)

    /** The token of the most recent invitation mailed to [email]. */
    protected fun tokenMailedTo(email: String): String =
        emails.sent.last { it.to == email.trim().lowercase() }.body
            .substringAfter("token=").lineSequence().first().trim()

    protected fun accept(
        token: String,
        name: String = "New Member",
        password: String = "a-long-passphrase-1",
    ): ResultActionsDsl =
        mockMvc.post("/v1/admin/invitations/accept") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("token" to token, "name" to name, "password" to password))
        }

    /** Invites [email] and accepts the invitation: the person ends up as staff with [password]. */
    protected fun onboard(
        adminToken: String,
        email: String,
        role: Role = Role.SUPPORT,
        password: String = "a-long-passphrase-1",
    ) {
        invite(adminToken, email, role).andExpect { status { isCreated() } }
        accept(tokenMailedTo(email), password = password).andExpect { status { isCreated() } }
    }

    protected fun changeRoleOf(
        adminToken: String,
        id: Long,
        role: Role,
    ): ResultActionsDsl =
        mockMvc.patch("/v1/admin/staff/$id/role") {
            header("Authorization", "Bearer $adminToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("role" to role.name))
        }

    protected fun blockStaff(
        adminToken: String,
        id: Long,
    ): ResultActionsDsl =
        mockMvc.post("/v1/admin/staff/$id/block") {
            header("Authorization", "Bearer $adminToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("reason" to "left the company"))
        }

    protected fun unblockStaff(
        adminToken: String,
        id: Long,
    ): ResultActionsDsl = mockMvc.post("/v1/admin/staff/$id/unblock") { header("Authorization", "Bearer $adminToken") }

    protected fun listInvitations(adminToken: String): MvcResult =
        mockMvc.get("/v1/admin/invitations") { header("Authorization", "Bearer $adminToken") }.andReturn()

    protected fun revokeInvitation(
        adminToken: String,
        id: Long,
    ): ResultActionsDsl = mockMvc.delete("/v1/admin/invitations/$id") { header("Authorization", "Bearer $adminToken") }

    protected fun accessTokenOf(result: MvcResult): String = json(result)["accessToken"].asText()
}
