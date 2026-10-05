package com.dbook.presentation.lifecycle

import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals

class EveryRequestIsCountedByTheAppThatMadeItTest : LifecycleFixture() {
    @Test
    fun `given an Android app on 1_4_2 when it calls the API then the request is counted under android and 1_4`() {
        val before = count("dbook.app.requests", "platform", "android", "appVersion", "1.4")

        mockMvc.get("/v1/app-config") {
            header("X-App-Version", "1.4.2+17")
            header("X-App-Platform", "Android")
        }

        assertEquals(before + 1, count("dbook.app.requests", "platform", "android", "appVersion", "1.4"))
    }

    @Test
    fun `given no or hostile headers when a request arrives then it is counted as unknown, not as a new series`() {
        val before = count("dbook.app.requests", "platform", "unknown", "appVersion", "unknown")

        mockMvc.get("/v1/app-config")
        mockMvc.get("/v1/app-config") {
            header("X-App-Version", "9999.9999.9999")
            header("X-App-Platform", "totally-made-up")
        }

        assertEquals(before + 2, count("dbook.app.requests", "platform", "unknown", "appVersion", "unknown"))
    }
}
