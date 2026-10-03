package com.dbook.infrastructure.web.requestloggingfilter

import com.dbook.infrastructure.observability.ObservabilityFixture
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.logging.LoggingSystem
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.test.context.ActiveProfiles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// What production (profile "json") actually writes: one JSON object per line, with the access
// data as separate fields, so CloudWatch Logs Insights / Loki can filter on them.
@ActiveProfiles("json")
@ExtendWith(OutputCaptureExtension::class)
class JsonProfileWritesStructuredLogsTest : ObservabilityFixture() {
    @Test
    fun `given the json profile when a request is served then its access line is a JSON object with the fields`(
        output: CapturedOutput,
    ) {
        getFromApi("/health", mapOf("X-Request-Id" to "json-req-1"))

        val line: JsonNode? =
            output.out.lines()
                .filter { it.startsWith("{") }
                .map { ObjectMapper().readTree(it) }
                .firstOrNull {
                    it["requestId"]?.asText() == "json-req-1" && it["message"].asText().startsWith("GET /health")
                }

        assertNotNull(line, "no JSON access line for the request in the captured output")
        assertEquals("INFO", line["level"].asText())
        assertEquals("dbook", line["app"].asText())
        assertEquals(200, line["status"].asInt())
        assertTrue(line["duration_ms"].isNumber, "duration_ms should be a number")
        assertTrue(line.has("@timestamp"))
    }

    companion object {
        // Spring Boot configures Logback once per JVM: if another test's context started first,
        // the "json" profile would be ignored and this test would depend on the order the suite
        // runs in. Marking logging as uninitialized makes THIS context configure it again, and
        // again afterwards so the contexts that follow are not left with JSON.
        @JvmStatic
        @BeforeAll
        fun forceLoggingToBeConfiguredAgain() = LoggingSystem.get(ClassLoader.getSystemClassLoader()).cleanUp()

        @JvmStatic
        @AfterAll
        fun releaseLoggingConfiguration() = LoggingSystem.get(ClassLoader.getSystemClassLoader()).cleanUp()
    }
}
