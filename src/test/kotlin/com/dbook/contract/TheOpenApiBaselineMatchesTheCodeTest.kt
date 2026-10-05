package com.dbook.contract

import com.dbook.AbstractIntegrationTest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

// The contract of each API version, as the code publishes it, is committed in docs/openapi. CI compares the committed
// file of a pull request with the one on main (oasdiff) and refuses a breaking change; this test keeps the committed
// file honest, so a change to the API cannot reach main without showing in a diff of the contract.
// To accept an intended change: UPDATE_OPENAPI=true ./gradlew test --tests '*OpenApiBaseline*'
@AutoConfigureMockMvc
class TheOpenApiBaselineMatchesTheCodeTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private fun published(group: String): String {
        val body = mockMvc.get("/v3/api-docs/$group").andReturn().response.getContentAsString(Charsets.UTF_8)
        // read as plain maps and lists (a JsonNode tree would keep the order springdoc happened to emit, which is not
        // stable: the response codes of an operation came out as 200, 202 in one run and 202, 200 in the next)
        val sorted = objectMapper.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        val plain = sorted.readValue(body, Any::class.java)
        return sorted.writerWithDefaultPrettyPrinter().writeValueAsString(plain) + "\n"
    }

    @Test
    fun `given each API version when its contract is published then it equals the committed baseline`() {
        listOf("v1", "v2").forEach { group ->
            val file = File("docs/openapi/openapi-$group.json")
            val current = published(group)
            if (System.getenv("UPDATE_OPENAPI") == "true") {
                file.writeText(current)
            }
            assertEquals(
                file.takeIf { it.exists() }?.readText(),
                current,
                "openapi-$group.json is out of date: UPDATE_OPENAPI=true ./gradlew test --tests '*OpenApiBaseline*'",
            )
        }
    }
}
