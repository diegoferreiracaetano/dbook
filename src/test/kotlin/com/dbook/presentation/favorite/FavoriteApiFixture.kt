package com.dbook.presentation.favorite

import com.dbook.presentation.securityintegration.SecurityIntegrationFixture
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put

abstract class FavoriteApiFixture : SecurityIntegrationFixture() {
    protected fun aCustomer(): Pair<String, Long> {
        val email = uniqueEmail()
        return registerAndLogin(email) to userIdOf(email)
    }

    protected fun body(result: MvcResult): JsonNode =
        objectMapper.readTree(result.response.getContentAsString(Charsets.UTF_8))

    protected fun save(
        token: String,
        type: String,
        id: String,
    ): MvcResult = mockMvc.put("/v1/favorites/$type/$id") { header("Authorization", "Bearer $token") }.andReturn()

    protected fun remove(
        token: String,
        type: String,
        id: String,
    ): MvcResult = mockMvc.delete("/v1/favorites/$type/$id") { header("Authorization", "Bearer $token") }.andReturn()

    protected fun list(
        token: String,
        vararg params: Pair<String, String>,
    ): MvcResult =
        mockMvc.get("/v1/favorites") {
            header("Authorization", "Bearer $token")
            params.forEach { (name, value) -> param(name, value) }
        }.andReturn()

    protected fun listed(
        token: String,
        vararg params: Pair<String, String>,
    ): List<String> = body(list(token, *params))["items"].map { "${it["type"].asText()}:${it["id"].asText()}" }

    protected fun count(userId: Long): Int =
        jdbcTemplate.queryForObject("SELECT count(*) FROM favorite WHERE user_id = ?", Int::class.java, userId) ?: 0
}
