package com.dbook.presentation.lifecycle

import org.springframework.test.web.servlet.get
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ADeprecatedEndpointTellsItsClientsAndIsCountedTest : LifecycleFixture() {
    @Test
    fun `given a deprecated endpoint when called then the response carries Deprecation, Sunset and Link`() {
        val response = mockMvc.get("/v1/destinations").andReturn().response

        val since = LocalDate.of(2026, 10, 5).atStartOfDay(ZoneOffset.UTC).toEpochSecond()
        assertEquals("@$since", response.getHeader("Deprecation"))
        assertEquals("Tue, 05 Oct 2027 00:00:00 GMT", response.getHeader("Sunset"))
        assertTrue(response.getHeader("Link")!!.endsWith("""#o-ensaio-de-uma-v2>; rel="deprecation""""))
    }

    @Test
    fun `given the deprecated endpoint when an app calls it then the call is counted by path and app version`() {
        val before = count("dbook.api.deprecated.calls", "path", "/v1/destinations", "appVersion", "1.4")

        repeat(2) { mockMvc.get("/v1/destinations") { header("X-App-Version", "1.4.2+17") } }
        mockMvc.get("/v1/destinations") { header("X-App-Version", "not a version") }

        assertEquals(before + 2, count("dbook.api.deprecated.calls", "path", "/v1/destinations", "appVersion", "1.4"))
        assertTrue(count("dbook.api.deprecated.calls", "path", "/v1/destinations", "appVersion", "unknown") >= 1.0)
    }

    @Test
    fun `given endpoints that are not deprecated when called then they carry no deprecation headers`() {
        val v2 = mockMvc.get("/v2/destinations").andReturn().response
        val search = mockMvc.get("/v1/app-config").andReturn().response

        assertNull(v2.getHeader("Deprecation"))
        assertNull(v2.getHeader("Sunset"))
        assertNull(search.getHeader("Deprecation"))
    }
}
