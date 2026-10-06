package com.dbook.presentation.customersearch

import com.dbook.presentation.securityintegration.SecurityIntegrationFixture
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.get

// Customers are created with a random tag in their name and searched by it: the database is shared by every
// integration test, so a search without the tag would see other tests' customers.
abstract class CustomerSearchFixture : SecurityIntegrationFixture() {
    protected fun newTag() = "zq${(100_000..999_999).random()}"

    protected fun newCustomer(name: String): String {
        val email = uniqueEmail()
        registerAndLogin(email, name = name)
        return email
    }

    protected fun supportToken(): String =
        registerStaffAndLogin(
            uniqueEmail(),
            com.dbook.domain.common.access.Role.SUPPORT,
        )

    protected fun search(
        token: String,
        vararg params: Pair<String, String>,
    ): MvcResult =
        mockMvc.get("/v1/admin/customers") {
            header("Authorization", "Bearer $token")
            params.forEach { (name, value) -> param(name, value) }
        }.andReturn()

    // JSON is UTF-8 on the wire; MockMvc would otherwise decode the body as ISO-8859-1 and turn "é" into "Ã©"
    protected fun bodyOf(result: MvcResult): JsonNode =
        objectMapper.readTree(result.response.getContentAsString(Charsets.UTF_8))

    protected fun namesOf(result: MvcResult): List<String> = bodyOf(result)["items"].map { it["name"].asText() }
}
