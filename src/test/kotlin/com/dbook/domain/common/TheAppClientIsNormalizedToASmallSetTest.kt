package com.dbook.domain.common

import kotlin.test.Test
import kotlin.test.assertEquals

class TheAppClientIsNormalizedToASmallSetTest {
    @Test
    fun `given real version strings when normalized then only the major and the minor are kept`() {
        assertEquals("1.4", AppClient.version("1.4.2"))
        assertEquals("1.4", AppClient.version("1.4.2+17"))
        assertEquals("2.0", AppClient.version(" 2.0.0-beta.1 "))
        assertEquals("10.12", AppClient.version("10.12"))
    }

    @Test
    fun `given garbage, hostile or missing versions when normalized then they all become unknown`() {
        listOf(
            null, "", "abc", "1", "1.x", "9999.1.1", "1.4.2\nX-Evil: 1", "1.4.2;drop table",
            "a".repeat(
                200,
            ),
        ).forEach {
            assertEquals("unknown", AppClient.version(it), "version of '$it'")
        }
    }

    @Test
    fun `given a platform when normalized then it is android, ios, web or unknown`() {
        assertEquals("android", AppClient.platform("Android"))
        assertEquals("ios", AppClient.platform(" iOS "))
        assertEquals("web", AppClient.platform("web"))
        assertEquals("unknown", AppClient.platform("windows-phone"))
        assertEquals("unknown", AppClient.platform(null))
    }
}
