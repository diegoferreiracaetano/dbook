package com.dbook.presentation.lifecycle

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals

class TheAppAsksWhichVersionsAreServedBeforeAnythingElseTest : LifecycleFixture() {
    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `given no sign-in when the app asks for its config then it gets the minimum and the latest per platform`() {
        val result = mockMvc.get("/v1/app-config").andReturn()

        assertEquals(200, result.response.status)
        val json = objectMapper.readTree(result.response.contentAsString)
        assertEquals("0.0.0", json["android"]["minSupportedVersion"].asText())
        assertEquals("0.0.0", json["ios"]["latestVersion"].asText())
        assertEquals("", json["android"]["storeUrl"].asText())
    }
}
